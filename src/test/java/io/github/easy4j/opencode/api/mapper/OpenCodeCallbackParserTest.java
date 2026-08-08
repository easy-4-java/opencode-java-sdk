package io.github.easy4j.opencode.api.mapper;

import io.github.easy4j.opencode.api.model.Part;
import io.github.easy4j.opencode.api.model.PromptResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpenCodeCallbackParser}.
 */
class OpenCodeCallbackParserTest {

    private final OpenCodeCallbackParser parser = new OpenCodeCallbackParser();

    @Test
    void shouldReturnNullForNullResult() {
        assertNull(parser.parseFromPromptResult(null));
    }

    @Test
    void shouldReturnNullForNullParts() {
        PromptResult result = new PromptResult();
        result.setParts(null);
        assertNull(parser.parseFromPromptResult(result));
    }

    @Test
    void shouldReturnNullForEmptyText() {
        Part part = new Part();
        part.setType("text");
        part.setText("");
        PromptResult result = new PromptResult();
        result.setParts(Collections.singletonList(part));
        assertNull(parser.parseFromPromptResult(result));
    }

    @Test
    void shouldParseJsonFromCodeBlock() {
        String text = "Here is the result:\n```json\n{\"task_id\":\"123\",\"status\":\"done\"}\n```\nDone.";
        Map<String, Object> parsed = parser.parseFromText(text);
        assertNotNull(parsed);
        assertEquals("123", parsed.get("task_id"));
        assertEquals("done", parsed.get("status"));
    }

    @Test
    void shouldParseDirectJson() {
        String text = "{\"title\":\"My Task\",\"value\":42}";
        Map<String, Object> parsed = parser.parseFromText(text);
        assertNotNull(parsed);
        assertEquals("My Task", parsed.get("title"));
        assertEquals(42, parsed.get("value"));
    }

    @Test
    void shouldParseBareJsonWithTaskId() {
        String text = "Some text before {\"task_id\":\"abc\",\"data\":\"test\"} some text after";
        Map<String, Object> parsed = parser.parseFromText(text);
        assertNotNull(parsed);
        assertEquals("abc", parsed.get("task_id"));
    }

    @Test
    void shouldParseBareJsonWithTaskIdCamelCase() {
        String text = "prefix {\"taskId\":\"xyz\"} suffix";
        Map<String, Object> parsed = parser.parseFromText(text);
        assertNotNull(parsed);
        assertEquals("xyz", parsed.get("taskId"));
    }

    @Test
    void shouldParseBareJsonWithTitle() {
        String text = "prefix {\"title\":\"My Title\"} suffix";
        Map<String, Object> parsed = parser.parseFromText(text);
        assertNotNull(parsed);
        assertEquals("My Title", parsed.get("title"));
    }

    @Test
    void shouldReturnNullForUnparsableText() {
        assertNull(parser.parseFromText("just plain text with no json"));
    }

    @Test
    void shouldReturnNullForNullText() {
        assertNull(parser.parseFromText(null));
    }

    @Test
    void shouldReturnNullForEmptyTextInput() {
        assertNull(parser.parseFromText(""));
    }

    @Test
    void shouldParseFromPromptResultWithValidText() {
        Part part = new Part();
        part.setType("text");
        part.setText("{\"taskId\":\"r1\",\"title\":\"result\"}");
        PromptResult result = new PromptResult();
        result.setParts(Collections.singletonList(part));

        Map<String, Object> parsed = parser.parseFromPromptResult(result);
        assertNotNull(parsed);
        assertEquals("r1", parsed.get("taskId"));
    }

    @Test
    void shouldReturnNullForBareJsonWithoutRequiredFields() {
        String text = "prefix {\"foo\":\"bar\"} suffix";
        assertNull(parser.parseFromText(text));
    }
}
