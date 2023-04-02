package juuxel.spindle.util;

import net.fabricmc.loader.impl.util.log.LogCategory;

public final class LogCategories {
    public static final LogCategory GENERAL = create();
    public static final LogCategory LOADING = create("Loading");
    public static final LogCategory MODULES = create("Modules");

    private static LogCategory create(String... parts) {
        return LogCategory.createCustom("Spindle", parts);
    }
}
