package juuxel.spindle.util;

import org.jetbrains.annotations.Nullable;

public final class AccessibleClassLoader extends ClassLoader {
    public AccessibleClassLoader(String name, ClassLoader parent) {
        super(name, parent);
    }

    public @Nullable Class<?> findLoadedClassExt(String name) {
        return findLoadedClass(name);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    static {
        registerAsParallelCapable();
    }
}
