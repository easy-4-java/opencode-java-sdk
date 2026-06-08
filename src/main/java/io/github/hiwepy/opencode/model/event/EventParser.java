package io.github.hiwepy.opencode.model.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.hiwepy.opencode.model.Event;

/**
 * 事件类型解析工具，将泛化 {@link Event} 转换为具体类型。
 */
public final class EventParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EventParser() {}

    /**
     * 根据事件 type 将泛化 Event 转换到对应的类型化事件。
     */
    @SuppressWarnings("unchecked")
    public static <T extends TypedEvent> T parse(Event event, Class<T> targetType) {
        TypedEvent typed;
        try {
            typed = MAPPER.convertValue(event, targetType);
        } catch (Exception e) {
            typed = new TypedEvent();
            typed.setType(event.getType());
            typed.setProperties(event.getProperties());
        }
        return (T) typed;
    }

    /**
     * 不指定类型，仅包装为 TypedEvent。
     */
    public static TypedEvent parse(Event event) {
        TypedEvent typed = new TypedEvent();
        typed.setType(event.getType());
        typed.setProperties(event.getProperties());
        return typed;
    }

    /** 是否为文本增量事件 */
    public static boolean isTextDelta(Event event) {
        return event != null && "session.next.text.delta".equals(event.getType());
    }

    /** 是否为工具调用事件 */
    public static boolean isToolEvent(Event event) {
        if (event == null) return false;
        String t = event.getType();
        return "session.next.tool.called".equals(t)
                || "session.next.tool.success".equals(t)
                || "session.next.tool.failed".equals(t);
    }

    /** 是否为步骤事件 */
    public static boolean isStepEvent(Event event) {
        if (event == null) return false;
        String t = event.getType();
        return "session.next.step.started".equals(t)
                || "session.next.step.ended".equals(t)
                || "session.next.step.failed".equals(t);
    }

    /** 是否为推理增量事件 */
    public static boolean isReasoningDelta(Event event) {
        return event != null && "session.next.reasoning.delta".equals(event.getType());
    }
}
