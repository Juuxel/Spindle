package juuxel.spindle.util;

public interface ClassClearer<T> {
    void clear(T t);

    static <T> ClassClearer<T> forClass(Class<T> c) {
        return ClassClearerImpl.getClearer(c);
    }
}
