package com.kinlik.aicodehelper.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmotionAnalysisService {

    @Value("${langchain4j.community.dashscope.chat-model.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmotionResult analyzeEmotion(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("情感分析输入为空，返回默认值");
            return new EmotionResult("平静", 5, "中性", false);
        }

        log.info("准备分析情绪文本: {}", text);

        try {
            String systemPrompt = """
                你是一个情感分析专家。请分析用户文本的情感状态，严格返回JSON格式（不要包含markdown代码块标记）：
                {
                  "emotion": "情绪类型（开心/焦虑/疲惫/兴奋/沮丧/平静/愤怒/担忧）",
                  "intensity": 强度值(1-10的整数),
                  "sentiment": "正面/负面/中性",
                  "needs_support": true或false
                }
                """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> input = new HashMap<>();
            input.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", text)
            ));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "qwen-max");
            requestBody.put("input", input);
            requestBody.put("parameters", Map.of("result_format", "message"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("调用DashScope API，请求体: {}", requestBody);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation",
                    entity,
                    String.class
            );

            log.debug("DashScope响应: {}", response.getBody());

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("output")
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            log.debug("解析后的内容: {}", content);

            JsonNode emotionJson = objectMapper.readTree(content);

            EmotionResult result = new EmotionResult(
                    emotionJson.path("emotion").asText("平静"),
                    emotionJson.path("intensity").asInt(5),
                    emotionJson.path("sentiment").asText("中性"),
                    emotionJson.path("needs_support").asBoolean(false)
            );

            log.info("情感分析结果: emotion={}, intensity={}, sentiment={}",
                    result.emotion(), result.intensity(), result.sentiment());

            return result;

        } catch (HttpClientErrorException e) {
            log.error("情感分析API调用失败 | 状态码: {} | 错误详情: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return new EmotionResult("平静", 5, "中性", false);
        } catch (Exception e) {
            log.error("情感分析失败", e);
            return new EmotionResult("平静", 5, "中性", false);
        }
    }

    public record EmotionResult(String emotion, int intensity, String sentiment, boolean needsSupport) {}
}
