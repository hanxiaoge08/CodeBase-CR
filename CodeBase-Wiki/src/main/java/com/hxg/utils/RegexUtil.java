package com.hxg.utils;

import org.springframework.util.ObjectUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author hxg
 * @date 2025/7/23 15:34
 */
public class RegexUtil {
    /**
     * 从文本中提取指定XML标签中的内容
     *
     * @param text 包含标签的文本
     * @param tagName1 标签名称（不包含尖括号）
     * @return 标签中的内容，如果没有找到则返回null
     */
    public static String extractXmlTagContent(String text, String tagName1, String tagName2) {
        if (ObjectUtils.isEmpty(text) ||ObjectUtils.isEmpty(tagName1) || ObjectUtils.isEmpty(tagName2)) {
            return null;
        }

        // 动态构建正则表达式，Pattern.quote确保tagName中的特殊字符被正确转义
        String patternStr = tagName1 + "\\s*([\\s\\S]*?)\\s*" + tagName2;
        Pattern pattern = Pattern.compile(patternStr);

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}
