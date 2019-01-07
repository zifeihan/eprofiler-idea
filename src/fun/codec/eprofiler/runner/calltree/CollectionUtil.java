package fun.codec.eprofiler.runner.calltree;

import java.util.Collection;

/**
 * @author: echo
 * @create: 2018-12-12 22:28
 */
public class CollectionUtil {
    public static boolean isEmpty(Collection collection) {
        return collection != null && collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection collection) {
        return !isEmpty(collection);
    }
}
