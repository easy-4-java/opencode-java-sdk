package io.github.easy4j.opencode;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JDK 8 测试集合工厂，替代 JDK 9 引入的集合静态工厂。
 */
public final class Java8Collections {

    private Java8Collections() {
    }

    @SafeVarargs
    public static <T> List<T> list(T... values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> map(Object... values) {
        Map<K, V> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put((K) values[index], (V) values[index + 1]);
        }
        return Collections.unmodifiableMap(result);
    }

    @SafeVarargs
    public static <T> Set<T> set(T... values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(values)));
    }
}
