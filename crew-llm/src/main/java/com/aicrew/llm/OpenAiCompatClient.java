package com.aicrew.llm;

import com.aicrew.common.entity.ModelConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * OpenAI 兼容协议客户端（/chat/completions）。
 * 兼容 DeepSeek、智谱 GLM、通义 Qwen、硅基流动、OpenAI 及各类中转站。
 */
@Slf4j
@Component
public class OpenAiCompatClient implements LlmClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public String chat(ModelConfig cfg, String systemPrompt, String userPrompt) {
        String url = normalize(cfg.getBaseUrl()) + "/chat/completions";
        String body = buildBody(cfg, systemPrompt, userPrompt);

        IOException lastIo = null;
        // 网络抖动重试一次
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(180))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + cfg.getApiKey())
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 400) {
                    throw new LlmException("模型「" + cfg.getName() + "」调用失败", resp.statusCode(), resp.body());
                }
                return extractContent(resp.body(), cfg.getName());
            } catch (IOException e) {
                lastIo = e;
                log.warn("模型 {} 第 {} 次请求网络异常: {}", cfg.getName(), attempt, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LlmException("模型调用被中断");
            }
        }
        throw new LlmException("模型「" + cfg.getName() + "」网络异常：" + (lastIo == null ? "未知" : lastIo.getMessage()));
    }

    private String buildBody(ModelConfig cfg, String systemPrompt, String userPrompt) {
        ObjectNode root = om.createObjectNode();
        root.put("model", cfg.getModel());
        root.put("stream", false);
        if (cfg.getTemperature() != null) {
            root.put("temperature", cfg.getTemperature());
        }
        if (cfg.getMaxTokens() != null && cfg.getMaxTokens() > 0) {
            root.put("max_tokens", cfg.getMaxTokens());
        }
        ArrayNode messages = root.putArray("messages");
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            ObjectNode sys = messages.addObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
        }
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt);
        return root.toString();
    }

    private String extractContent(String respBody, String modelName) {
        try {
            JsonNode root = om.readTree(respBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                JsonNode errMsg = root.path("error").path("message");
                throw new LlmException("模型「" + modelName + "」返回异常：" +
                        (errMsg.isMissingNode() ? respBody : errMsg.asText()));
            }
            return content.asText();
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("模型「" + modelName + "」响应解析失败：" + e.getMessage());
        }
    }

    /** 去掉末尾斜杠，允许填写到 /v1 或厂商根路径 */
    static String normalize(String baseUrl) {
        String s = baseUrl == null ? "" : baseUrl.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
