/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.classpath;

import java.net.URI;
import java.util.stream.Stream;

/**
 * A wrapper for a classpath that has an {@linkplain #getTargetClassLoader() associated class loader}.
 *
 * @see ModuleClasspath
 */
public interface Classpath {
    /**
     * {@return a stream of all code sources on this classpath}
     */
    Stream<URI> codeSources();

    /**
     * {@return a class loader accessing classes and resources on this classpath}
     */
    ClassLoader getTargetClassLoader();

    /**
     * {@return {@code true} if the specified class is loaded, {@code false} otherwise}
     *
     * @param name the class name to check
     */
    boolean isClassLoaded(String name);

    /**
     * Loads the specified class into the target class loader.
     *
     * @param name the class name
     * @return the loaded class
     */
    Class<?> loadIntoTarget(String name) throws ClassNotFoundException;
}
