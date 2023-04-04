/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.classpath;

import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;

public final class LazyClasspath implements Classpath {
    private Factory factory;
    private Classpath real;
    private Classpath fallback;

    public LazyClasspath(Factory factory, Classpath fallback) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    private Classpath getEffective() {
        Classpath result = real;

        if (result == null) {
            try {
                result = real = Objects.requireNonNull(factory.create(),
                    "Factory.create returned null classpath");

                // Clear there out so they can get gc'd
                factory = null;
                fallback = null;
            } catch (ClasspathCreationException e) {
                // obviously didn't work
                result = fallback;
            }
        }

        return result;
    }

    @Override
    public Stream<URI> codeSources() {
        return getEffective().codeSources();
    }

    @Override
    public ClassLoader getTargetClassLoader() {
        return getEffective().getTargetClassLoader();
    }

    @Override
    public boolean isClassLoaded(String name) {
        return getEffective().isClassLoaded(name);
    }

    @Override
    public Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
        return getEffective().loadIntoTarget(name);
    }

    @FunctionalInterface
    public interface Factory {
        Classpath create() throws ClasspathCreationException;
    }
}
