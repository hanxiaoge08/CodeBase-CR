# server.py
import re
from typing import List, Optional, Tuple

import uvicorn
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from tree_sitter_languages import get_parser

app = FastAPI(title="AST Chunk Service", version="2.3")

# ===================== I/O Schema =====================

class ParseRequest(BaseModel):
    language: Optional[str] = Field(None, description="java/python/go/js/ts/c/cpp/c_sharp/rust")
    code: Optional[str] = None
    max_chars: int = Field(2000, gt=0, description="当方法源码超过该长度时追加切片块（仍为 subType=function）")
    # 兼容老字段
    type: Optional[str] = None
    text: Optional[str] = None

class Chunk(BaseModel):
    # —— 统一字段（即使为 null 也输出）——
    id: str
    language: str
    subType: Optional[str] = None          # 仅使用 "function"
    className: Optional[str] = None
    methodName: Optional[str] = None
    apiName: Optional[str] = None
    docSummary: Optional[str] = None
    content: str

class ParseResponse(BaseModel):
    chunks: List[Chunk]

# ===================== Language Specs =====================

def S(*xs): return set(xs)

LANG_SPECS = {
    "java": {
        "atom":  S("class_declaration","interface_declaration","enum_declaration",
                   "method_declaration","constructor_declaration"),
        "func":  S("method_declaration","constructor_declaration"),
        "klass": S("class_declaration","interface_declaration","enum_declaration"),
        "head":  S("package_declaration","import_declaration"),  # 仅用于注释定位，不产出块
    },
    # 其他语言保留以便拓展（当前主要用 java）
    "python": {
        "atom":  S("class_definition","function_definition"),
        "func":  S("function_definition"),
        "klass": S("class_definition"),
        "head":  S("import_statement","import_from_statement"),
    },
    "javascript": {
        "atom":  S("class_declaration","function_declaration","method_definition"),
        "func":  S("function_declaration","method_definition"),
        "klass": S("class_declaration"),
        "head":  S("import_statement"),
    },
    "typescript": {
        "atom":  S("class_declaration","function_declaration","method_definition"),
        "func":  S("function_declaration","method_definition"),
        "klass": S("class_declaration"),
        "head":  S("import_statement"),
    },
    "go": {
        "atom":  S("type_declaration","function_declaration","method_declaration"),
        "func":  S("function_declaration","method_declaration"),
        "klass": S("type_declaration"),
        "head":  S("import_declaration"),
    },
    "c": {
        "atom":  S("function_definition"),
        "func":  S("function_definition"),
        "klass": S(),
        "head":  S("preproc_include"),
    },
    "cpp": {
        "atom":  S("class_specifier","function_definition"),
        "func":  S("function_definition"),
        "klass": S("class_specifier"),
        "head":  S("preproc_include","namespace_definition"),
    },
    "c_sharp": {
        "atom":  S("class_declaration","struct_declaration","interface_declaration",
                   "method_declaration","constructor_declaration"),
        "func":  S("method_declaration","constructor_declaration"),
        "klass": S("class_declaration","struct_declaration","interface_declaration"),
        "head":  S("using_directive","namespace_declaration"),
    },
    "rust": {
        "atom":  S("struct_item","enum_item","trait_item","impl_item","function_item"),
        "func":  S("function_item"),
        "klass": S("struct_item","enum_item","trait_item","impl_item"),
        "head":  S("use_declaration","extern_crate_declaration","mod_item"),
    },
}

# ===================== Helpers =====================

COMMENT_TRAILER_RE = re.compile(
    r"(?:/\*\*?[\s\S]*?\*/|(?://[^\n]*\n?)+)\s*$",
    re.MULTILINE
)

def node_text(src: bytes, n) -> str:
    return src[n.start_byte:n.end_byte].decode("utf-8", errors="ignore")

def java_identifier_of(node) -> Optional[str]:
    for ch in node.children:
        if ch.type == "identifier":
            try:
                return ch.text.decode("utf-8", "ignore")
            except Exception:
                return None
    return None

def enclosing_java_class_and_method(node) -> Tuple[Optional[str], Optional[str]]:
    cur = node
    cls = None
    while cur is not None:
        if cur.type in ("class_declaration","interface_declaration","enum_declaration"):
            cls = java_identifier_of(cur)
            break
        cur = getattr(cur, "parent", None)
    m = None
    if node.type in ("method_declaration","constructor_declaration"):
        m = java_identifier_of(node)   # 构造器名 == 类名
    return cls, m

def leading_comment_of(src: bytes, node) -> Optional[str]:
    """
    优先：regex 捕获紧邻的 Javadoc/块/行注释；
    回退：逐行向上收集（最多 8 行）注释（/**...*/, /*...*/, //... 连续块）。
    """
    start = node.start_byte
    window_start = max(0, start - 4000)
    seg = src[window_start:start].decode("utf-8", "ignore")

    # A: 正则尝试抓紧邻注释
    m = COMMENT_TRAILER_RE.search(seg)
    if m:
        gap = seg[m.end():]
        if "\n\n" not in gap:  # 没有空行隔断
            return m.group(0).strip()

    # B: 行级回溯
    lines = seg.splitlines()
    if not lines:
        return None
    picked: List[str] = []
    i = len(lines) - 1
    while i >= 0 and lines[i].strip() == "":
        i -= 1
    open_block = False
    count = 0
    while i >= 0 and count < 8:
        line = lines[i]
        s = line.lstrip()
        if open_block:
            picked.append(line)
            if "/*" in s or "/**" in s:
                open_block = False
            i -= 1; count += 1
            continue
        if s.startswith("//"):
            picked.append(line); i -= 1; count += 1; continue
        if "*/" in s:
            picked.append(line)
            open_block = True
            i -= 1; count += 1
            continue
        if s.startswith("/*") or s.startswith("/**"):
            picked.append(line); break
        break
    if picked:
        picked.reverse()
        return "\n".join(picked).strip()
    return None

def collect_atoms(root, spec):
    atoms = []
    stack = [root]
    while stack:
        n = stack.pop()
        if n.type in spec["atom"]:
            atoms.append(n)
        for i in range(len(n.children) - 1, -1, -1):
            stack.append(n.children[i])
    atoms.sort(key=lambda x: x.start_byte)
    return atoms

# ===================== Core (Function-Only, Unified Fields) =====================

def fill_api_for_method(language: str, node, src: bytes) -> dict:
    """
    生成统一字段（不返回 kind等），subType 固定为 "function"。
    """
    meta = {
        "subType": "function",
        "className": None,
        "methodName": None,
        "apiName": None,
        "docSummary": None,
    }
    if language == "java":
        cls, m = enclosing_java_class_and_method(node)
        meta["className"] = cls
        meta["methodName"] = m
        meta["apiName"] = f"{cls}#{m}" if cls and m else (m or cls)
        meta["docSummary"] = leading_comment_of(src, node)
    else:
        # 其他语言暂略（当前聚焦 java），保持统一字段
        meta["docSummary"] = leading_comment_of(src, node)
    return meta

def hard_split_method(text: str, language: str, base_node, src: bytes,
                      start_index: int, max_chars: int) -> List[Chunk]:
    """
    将超长方法 text 拆分成若干“切片块”（仍然是 subType=function），编号从 start_index+1 开始连续。
    """
    res: List[Chunk] = []
    idx = 0
    i = 0
    meta = fill_api_for_method(language, base_node, src)
    while idx < len(text):
        part = text[idx: idx + max_chars]
        i += 1
        res.append(Chunk(
            id=f"chunk:{start_index + i}",
            language=language,
            subType=meta["subType"],
            className=meta["className"],
            methodName=meta["methodName"],
            apiName=meta["apiName"],
            docSummary=meta["docSummary"],
            content=part
        ))
        idx += max_chars
    return res

def create_function_chunks(language: str, code: str, func_nodes, max_chars: int) -> List[Chunk]:
    """
    仅生成“函数/方法/构造器”的主块；必要时追加若干切片块（字段完全一致，只有 content 为分片）。
    不生成类块、不生成 preamble。
    """
    out: List[Chunk] = []
    src = code.encode("utf-8")
    id_counter = 0

    def add_main_block(txt: str, node):
        nonlocal id_counter
        id_counter += 1
        meta = fill_api_for_method(language, node, src)
        ch = Chunk(
            id=f"chunk:{id_counter}",
            language=language,
            subType=meta["subType"],
            className=meta["className"],
            methodName=meta["methodName"],
            apiName=meta["apiName"],
            docSummary=meta["docSummary"],
            content=txt
        )
        out.append(ch)
        return ch

    for fnode in func_nodes:
        txt = node_text(src, fnode)
        add_main_block(txt, fnode)

        # 超长 → 追加切片（依然 subType=function，且字段统一）
        if len(txt) > max_chars:
            slices = hard_split_method(
                txt, language, fnode, src, start_index=id_counter, max_chars=max_chars
            )
            out.extend(slices)
            id_counter += len(slices)

    return out

# ===================== API =====================

@app.get("/health")
def health():
    return {"ok": True}

# 不排除 None，让 null 字段也原样返回
@app.post("/parse", response_model=ParseResponse)
def parse(req: ParseRequest):
    # 兼容旧字段
    lang = (req.language or req.type or "").strip().lower()
    code = req.code if req.code is not None else (req.text or "")

    if not lang or not code:
        return ParseResponse(chunks=[])

    if lang not in LANG_SPECS:
        raise HTTPException(status_code=400, detail=f"Unsupported language: {lang}")

    try:
        parser = get_parser(lang)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"get_parser({lang}) failed: {e!r}")

    try:
        tree = parser.parse(code.encode("utf-8"))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"parser.parse failed: {e!r}")

    root = tree.root_node
    spec = LANG_SPECS[lang]

    # 仅保留“函数原子”，按源码顺序；不做“外层优先”去重（避免被类吞掉）
    all_atoms = collect_atoms(root, spec)
    func_nodes = [n for n in all_atoms if n.type in spec["func"]]

    if not func_nodes:
        return ParseResponse(chunks=[])

    chunks = create_function_chunks(lang, code, func_nodes, req.max_chars)
    return ParseResponse(chunks=chunks)

#source ../venv/bin/activate
#pip install -r requirements.txt
## 3) 后台启动，日志重定向到 ast.txt
# nohup python -m uvicorn server:app --host 0.0.0.0 --port 8566 --workers 2 > ast.txt 2>&1 & echo $! > ast.pid
if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8566)
