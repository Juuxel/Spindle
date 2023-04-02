package juuxel.spindle.util;

import org.jetbrains.annotations.Nullable;

public final class AccessibleClassLoader extends ClassLoader {
    public AccessibleClassLoader(String name, ClassLoader parent) {
        super(name, parent);
    }

    public @Nullable Class<?> findLoadedClassExt(String name) {
        return findLoadedClass(name);
    }

    static {
        registerAsParallelCapable();
    }
}
