/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.classpath;

import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A lazy classpath that tries to construct a classpath if it's available.
 * Otherwise, it uses a fallback.
 */
public final class LazyClasspath implements Classpath {
    private Factory factory;
    private Classpath real;
    private Classpath fallback;

    /**
     * Constructs a lazy classpath.
     *
     * @param factory  the factory of the primary target classpath
     * @param fallback the fallback target classpath
     */
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

    /**
     * A factory for classpaths.
     */
    @FunctionalInterface
    public interface Factory {
        /**
         * Creates a classpath.
         *
         * @return the created classpath
         * @throws ClasspathCreationException if creation did not succeed
         */
        Classpath create() throws ClasspathCreationException;
    }
}
