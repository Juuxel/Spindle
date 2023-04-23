/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.classpath;

/**
 * An exception that indicates that a {@linkplain Classpath} could not be created.
 */
public class ClasspathCreationException extends Exception {
    public ClasspathCreationException(String message) {
        super(message);
    }

    public ClasspathCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
