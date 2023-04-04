/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.classpath;

import java.net.URI;
import java.util.stream.Stream;

public interface Classpath {
    Stream<URI> codeSources();
    ClassLoader getTargetClassLoader();
    boolean isClassLoaded(String name);
    Class<?> loadIntoTarget(String name) throws ClassNotFoundException;
}
