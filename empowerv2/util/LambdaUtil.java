package empowerv2.util;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

// Workarounds to be not as bytecode excessive
public class LambdaUtil {
    // Replacement for Arrays.stream(array).max(comparator)
    public static <T> Optional<T> arraysStreamMax(T[] array, Comparator<T> comparator) {
        if (array.length == 0) {
            return Optional.empty();
        }
        int len_1 = array.length - 1;
        T best = array[len_1];
        for (int i = len_1; --i >= 0;) {
            T item = array[i];
            if (comparator.compare(item, best) > 0) {
                best = item;
            }
        }
        return Optional.of(best);
    }
    // Replacement for Arrays.stream(array).min(comparator)
    public static <T> Optional<T> arraysStreamMin(T[] array, Comparator<T> comparator) {
        if (array.length == 0) {
            return Optional.empty();
        }
        int len_1 = array.length - 1;
        T best = array[len_1];
        for (int i = len_1; --i >= 0;) {
            T item = array[i];
            if (comparator.compare(item, best) < 0) {
                best = item;
            }
        }
        return Optional.of(best);
    }
    // Replacement for Arrays.stream(array).filter(predicate).min(comparator)
    public static <T> Optional<T> arraysStreamMin(T[] array, Predicate<T> predicate, Comparator<T> comparator) {
        T best = null;
        for (int i = array.length; --i >= 0;) {
            T item = array[i];
            if (predicate.test(item) && (best == null || comparator.compare(item, best) < 0)) {
                best = item;
            }
        }
        return Optional.ofNullable(best);
    }
    // Java 8 doesn't have Optional.or()
    @SafeVarargs
    public static <T> Optional<T> or(Supplier<Optional<T>>... suppliers) {
        for (Supplier<Optional<T>> supplier : suppliers) {
            Optional<T> opt = supplier.get();
            if (opt.isPresent()) {
                return opt;
            }
        }
        return Optional.empty();
    }
}
