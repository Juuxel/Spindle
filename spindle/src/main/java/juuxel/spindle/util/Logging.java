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
