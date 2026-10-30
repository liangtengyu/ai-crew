package com.aicrew.common.util;

import com.aicrew.common.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 从大模型的自由文本中提取 JSON 对象。
 * 兼容模型常见的 ```json 围栏、前后废话等情况。
 */
public final class JsonUtil {

    private static final ObjectMapper OM = new ObjectMapper();

    private JsonUtil() {
    }

    public static String toJson(Object obj) {
        try {
            return OM.writeValueAsString(obj);
        } catch (Exception e) {
            throw new BizException("JSON 序列化失败: " + e.getMessage());
        }
    }

    /** 提取 JSON，失败返回 null */
    public static JsonNode extractJson(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String s = text.trim();
        // 去掉 markdown 代码块围栏
        s = s.replaceAll("^```(?:json|JSON)?\\s*", "").replaceAll("```\\s*$", "").trim();
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            return OM.readTree(s.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
    }
}
