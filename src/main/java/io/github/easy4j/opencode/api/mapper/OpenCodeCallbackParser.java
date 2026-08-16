package io.github.easy4j.opencode.api.mapper;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import io.github.easy4j.opencode.api.model.PromptResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses callback JSON from OpenCode AI text responses.
 * <p>OpenCode does not have a native webhook/callback mechanism. This parser attempts
 * to extract JSON blocks from the AI's text response, following the format conventions
 * defined by cloud-agents prompt templates (e.g., the callback_url output format
 * specified in SKILL.md).</p>
 * <p>The parser tries three strategies in order:</p>
 * <ol>
 *     <li>Extract JSON from {@code ```json ... ```} fenced code blocks</li>
 *     <li>Parse the entire text as JSON directly</li>
 *     <li>Extract bare JSON objects containing {@code task_id}, {@code taskId}, or {@code title}</li>
 * </ol>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see io.github.easy4j.opencode.api.model.PromptResult
 */
public class OpenCodeCallbackParser {

    /**
     * 当前组件使用的 SLF4J 日志记录器。
     */
    private static final Logger log = LoggerFactory.getLogger(OpenCodeCallbackParser.class);
    /**
     * 用于解析回调 JSON 的共享映射器，允许注释并忽略服务端新增字段。
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

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
     * @param result OpenCode Prompt 执行结果
     * @return OpenCode Server 返回的键值映射；无数据时为空映射
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
     * @param text 发送给模型的文本内容
     * @return OpenCode Server 返回的键值映射；无数据时为空映射
     */
    public Map<String, Object> parseFromText(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // 优先解析模型明确标记的 JSON 代码块，避免正文中的花括号被误识别为回调参数。
        Matcher matcher = JSON_BLOCK_PATTERN.matcher(text);
        if (matcher.find()) {
            String json = matcher.group(1);
            try {
                return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.debug("Failed to parse JSON from code block: contentLength={}", json.length(), e);
            }
        }

        // 其次接受整个响应就是 JSON 的简洁格式。
        try {
            return MAPPER.readValue(text.trim(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
        }

        // 最后才扫描裸 JSON，并要求存在业务识别字段，降低普通示例代码造成的误报。
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
