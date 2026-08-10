package io.github.easy4j.opencode.api;

import lombok.Builder;
import lombok.Value;

/**
 * OpenCode 单次请求上下文。
 *
 * <p>OpenCode 使用工作目录隔离项目配置、智能体和会话。调用方应传入经过授权和规范化的
 * 绝对目录，SDK 会将其写入 {@code X-OpenCode-Directory} 请求头。</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 2.7.x
 */
@Value
@Builder
public class OpenCodeRequestContext {

    /** OpenCode 工作目录。 */
    String directory;

    /**
     * 创建只包含工作目录的请求上下文。
     *
     * @param directory 工作目录
     * @return 请求上下文
     */
    public static OpenCodeRequestContext ofDirectory(String directory) {
        return OpenCodeRequestContext.builder().directory(directory).build();
    }
}
