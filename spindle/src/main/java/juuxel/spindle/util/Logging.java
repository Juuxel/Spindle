/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public final class Logging {
    public static final Logger LOGGER = LogManager.getLogger("Spindle");
    public static final Marker LOADING = marker("Loading");
    public static final Marker MODULES = marker("Modules");

    private static Marker marker(String... names) {
        return MarkerManager.getMarker(String.join("/", names));
    }
}
