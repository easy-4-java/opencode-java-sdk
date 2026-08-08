package io.github.easy4j.opencode.api.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.easy4j.opencode.api.model.PromptResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses callback JSON from OpenCode AI text responses.
 *
 * <p>OpenCode does not have a native webhook/callback mechanism. This parser attempts
 * to extract JSON blocks from the AI's text response, following the format conventions
 * defined by cloud-agents prompt templates (e.g., the callback_url output format
 * specified in SKILL.md).</p>
 *
 * <p>The parser tries three strategies in order:</p>
 * <ol>
 *     <li>Extract JSON from {@code ```json ... ```} fenced code blocks</li>
 *     <li>Parse the entire text as JSON directly</li>
 *     <li>Extract bare JSON objects containing {@code task_id}, {@code taskId}, or {@code title}</li>
 * </ol>
 *
 * @author [@Loong Wan](https://github.com/loong10k)
 * @since 3.0.0
 * @see io.github.easy4j.opencode.api.model.PromptResult
 */
public class OpenCodeCallbackParser {

    private static final Logger log = LoggerFactory.getLogger(OpenCodeCallbackParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * 匹配 ```json ... ``` 代码块中的 JSON。
     */
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile(
            "```json\\s*\\n?(\\{.*?})\\s*\\n?```", Pattern.DOTALL);

    /**
     * 匹配裸 JSON 对象（以 { 开头，以 } 结尾）。
     */
    private static final Pattern BARE_JSON_PATTERN = Pattern.compile(
            "(\\{[^{}]*(?:\\{[^{}]*}[^{}]*)*})", Pattern.DOTALL);

    /**
     * 从 PromptResult 中提取文本内容并解析为 Map。
     *
     * @param result prompt 响应
     * @return 解析后的 JSON Map，无法解析则返回 null
     */
    public Map<String, Object> parseFromPromptResult(PromptResult result) {
        if (result == null || result.getParts() == null) {
            return null;
        }
        String text = result.getTextContent();
        if (text == null || text.isEmpty()) {
            return null;
        }
        return parseFromText(text);
    }

    /**
     * 从纯文本中尝试提取并解析 JSON。
     *
     * @param text AI 响应文本
     * @return 解析后的 JSON Map，无法解析则返回 null
     */
    public Map<String, Object> parseFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // 尝试从 ```json ... ``` 代码块中提取
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            String json = matcher.group(1);
            try {
                return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.debug("Failed to parse JSON from code block: {}", json, e);
            }
        }

        // 尝试直接解析整个文本为 JSON
        try {
            return MAPPER.readValue(text.trim(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
        }

        // 尝试提取裸 JSON 对象
        Matcher bareMatcher = BARE_JSON_PATTERN.matcher(text);
        while (bareMatcher.find()) {
            String json = bareMatcher.group(1);
            try {
                Map<String, Object> parsed = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
                // 至少要有 task_id 或 title 字段才认为是有效回调
                if (parsed.containsKey("task_id") || parsed.containsKey("taskId") || parsed.containsKey("title")) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }
}
