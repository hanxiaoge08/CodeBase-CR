package com.hxg.service.impl;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxg.context.ExecutionContext;
import com.hxg.llm.prompt.AnalyzeCataloguePrompt;
import com.hxg.llm.prompt.GenDocPrompt;
import com.hxg.llm.service.LlmService;
import com.hxg.mapper.CatalogueMapper;
import com.hxg.model.dto.CatalogueStruct;
import com.hxg.model.dto.GenCatalogueDTO;
import com.hxg.model.entity.Catalogue;
import com.hxg.model.enums.CatalogueStatusEnum;
import com.hxg.model.vo.CatalogueListVo;
import com.hxg.service.ICatalogueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.hxg.utils.RegexUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author hxg
 * @description: 目录服务实现类
 * @date 2025/7/22 23:35
 */
@Slf4j
@Service
public class CatalogueServiceImpl extends ServiceImpl<CatalogueMapper, Catalogue> implements ICatalogueService {
    private final LlmService llmService;

    public CatalogueServiceImpl(LlmService llmService) {
        this.llmService = llmService;
    }

    @Override
    public GenCatalogueDTO generateCatalogue(String fileTree, ExecutionContext context) {
        //生成项目目录
        String genCataloguePrompt= AnalyzeCataloguePrompt.prompt
                .replace("{{$code_files}}",fileTree)
                .replace("{{$repository_location}}",context.getLocalPath());
        log.info("LLM开始生成项目目录，prompt内容：{}",genCataloguePrompt);
        String result=llmService.callWithTools(genCataloguePrompt);
        log.info("LLM生成项目目录结果：{}",result);
        
        //String documentationStructure= RegexUtil.extractXmlTagContent(result,"<documentation_structure>","</documentation_structure>");
        CatalogueStruct catalogueStruct = processCatalogueStruct(result);
        List<Catalogue> catalogueList = saveCatalogueStruct(context, catalogueStruct);
        return new GenCatalogueDTO(catalogueStruct,catalogueList);
    }

    @Override
    public CatalogueStruct processCatalogueStruct(String result) {
        try {
            // 首先解析外层JSON，检查是否包含documentation_structure字段
            JSONObject jsonObject = JSON.parseObject(result);
            
            CatalogueStruct catalogueStruct;
            
            // 如果包含documentation_structure字段，则提取该字段进行解析
            if (jsonObject.containsKey("documentation_structure")) {
                log.info("检测到documentation_structure包装，正在提取内容");
                JSONObject documentationStructure = jsonObject.getJSONObject("documentation_structure");
                catalogueStruct = documentationStructure.toJavaObject(CatalogueStruct.class);
            } else {
                // 如果没有包装，直接解析
                log.info("直接解析CatalogueStruct结构");
                catalogueStruct = JSON.parseObject(result, CatalogueStruct.class);
            }
            
            if (catalogueStruct.getItems() == null || catalogueStruct.getItems().isEmpty()) {
                log.error("解析LLM生成项目目录失败, LLM生成的目录为空");
                throw new RuntimeException("解析LLM生成项目目录失败, LLM生成的目录为空");
            }
            
            log.info("成功解析CatalogueStruct，包含{}个顶级项目", catalogueStruct.getItems().size());
            return catalogueStruct;
        } catch (JSONException e) {
            String msg = "解析LLM生成项目目录失败, LLM生成的目录格式不正确: " + e.getMessage();
            log.error(msg, e);
            throw new RuntimeException(msg);
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Catalogue> saveCatalogueStruct(ExecutionContext context, CatalogueStruct catalogueStruct) {
        log.info("保存项目目录到数据库：{}", catalogueStruct);
        List<Catalogue> saveList = new ArrayList<>();
        catalogueStruct.getItems().forEach(item -> {
            Catalogue catalogue = Catalogue.builder()
                    .catalogueId(UUID.fastUUID().toString())
                    .taskId(context.getTaskId())
                    .title(item.getTitle())
                    .name(item.getName())
                    .prompt(item.getPrompt())
                    .dependentFile(String.join(",", item.getDependent_file()))
                    .status(CatalogueStatusEnum.IN_PROGRESS.getCode())
                    .build();
            saveList.add(catalogue);
            
            item.getChildren().forEach(child -> {
                Catalogue childCatalogue = Catalogue.builder()
                        .catalogueId(UUID.fastUUID().toString())
                        .taskId(context.getTaskId())
                        .title(child.getTitle())
                        .name(child.getName())
                        .parentCatalogueId(catalogue.getCatalogueId())
                        .prompt(child.getPrompt())
                        .dependentFile(String.join(",", child.getDependent_file()))
                        .status(CatalogueStatusEnum.IN_PROGRESS.getCode())
                        .build();
                saveList.add(childCatalogue);
            });
        });
        // 使用逐个保存来确保能够获取到自增的ID
        saveList.forEach(this::save);
        
        return saveList;
    }

    @Override
    public void parallelGenerateCatalogueDetail(String fileTree, GenCatalogueDTO genCatalogueDTO, String localPath) {
        genCatalogueDTO.getCatalogueList().forEach(catalogue -> {
            if(StringUtils.isNotEmpty(catalogue.getParentCatalogueId())){
                generateCatalogueDetail(catalogue,fileTree,genCatalogueDTO.getCatalogueStruct(),localPath);
            }
        });
    }

    @Override
    @Async("GenCatalogueDetailExecutor")
    public void generateCatalogueDetail(Catalogue catalogue, String fileTree, CatalogueStruct catalogueStruct, String localPath) {
        try{
            log.info("LLM开始生成目录详情：{}", catalogue.getName());
            String prompt= GenDocPrompt.prompt
                    .replace("{{repository_location}}",localPath)
                    .replace("{{prompt}}",catalogue.getPrompt())
                    .replace("{{title}}",catalogue.getName())
                    .replace("{{$repository_files}}",fileTree)
                    .replace("{{$catalogue}}",JSON.toJSONString(catalogueStruct));
            String result=llmService.callWithTools(prompt);
            log.info("LLM生成{}目录详情结果：{}",catalogue.getName(),result);
            if(StringUtils.isEmpty(result)){
                throw new RuntimeException("LLM生成目录详情结果为空");
            }
            //保存目录详情
            catalogue.setContent(result);
            catalogue.setStatus(CatalogueStatusEnum.COMPLETED.getCode());
        }catch (Exception e){
            log.error("LLM生成{}目录详情失败",catalogue.getName(),e);
            catalogue.setStatus(CatalogueStatusEnum.FAILED.getCode());
            catalogue.setFailReason(e.getMessage());
        }finally {
            this.updateById(catalogue);
        }
    }

    @Override
    public void deleteCatalogueByTaskId(String taskId) {
        LambdaQueryWrapper<Catalogue> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Catalogue::getTaskId, taskId);
        this.remove(queryWrapper);
    }

    @Override
    public List<Catalogue> getCatalogueByTaskId(String taskId) {
        LambdaQueryWrapper<Catalogue> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Catalogue::getTaskId, taskId);
        queryWrapper.orderByAsc(Catalogue::getCreateTime);
        return this.list(queryWrapper);
    }

    /**
     * 根据taskId获取目录树形结构
     */
    @Override
    public List<CatalogueListVo> getCatalogueTreeByTaskId(String taskId) {
        LambdaQueryWrapper<Catalogue> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Catalogue::getTaskId, taskId);
        queryWrapper.orderByAsc(Catalogue::getCreateTime);
        List<Catalogue> catalogueList = this.list(queryWrapper);
        return buildCatalogueTree(catalogueList);
    }

    /**
     * 构建目录树形结构
     */
    private List<CatalogueListVo> buildCatalogueTree(List<Catalogue> catalogueList) {
        //转换为VO
        List<CatalogueListVo> allNodes = catalogueList.stream()
                .map(this::convertToCatalogueListVo)
                .toList();

        //构建父子关系映射
        Map<String, List<CatalogueListVo>> parentChildMap = allNodes.stream()
                .filter(vo -> StringUtils.isNotEmpty(vo.getParentCatalogueId()))
                .collect(Collectors.groupingBy(CatalogueListVo::getParentCatalogueId));

        // 设置子节点
        allNodes.forEach(node -> {
            List<CatalogueListVo> children = parentChildMap.get(node.getCatalogueId());
            node.setChildren(children != null ? children : new ArrayList<>());
        });

        // 返回根节点（没有父节点的节点）
        return allNodes.stream()
                .filter((vo -> StringUtils.isEmpty(vo.getParentCatalogueId())))
                .collect(Collectors.toList());
    }

    /**
     * 将Catalogue实体转换为CatalogueListVo
     */
    private CatalogueListVo convertToCatalogueListVo(Catalogue catalogue) {
        return CatalogueListVo.builder()
                .catalogueId(catalogue.getCatalogueId())
                .parentCatalogueId(catalogue.getParentCatalogueId())
                .name(catalogue.getName())
                .title(catalogue.getTitle())
                .prompt(catalogue.getPrompt())
                .dependentFile(catalogue.getDependentFile())
                .content(catalogue.getContent())
                .status(catalogue.getStatus())
                .build();
    }
}
