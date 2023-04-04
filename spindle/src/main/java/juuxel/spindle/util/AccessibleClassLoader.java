/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
