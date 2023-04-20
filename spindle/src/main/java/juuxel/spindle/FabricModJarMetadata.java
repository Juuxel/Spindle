/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle;

import cpw.mods.jarhandling.JarMetadata;
import juuxel.spindle.util.Logging;
import net.fabricmc.loader.api.ModContainer;

import java.lang.module.ModuleDescriptor;
import java.util.StringJoiner;

final class FabricModJarMetadata implements JarMetadata {
    private final ModContainer modContainer;
    private String name;
    private ModuleDescriptor descriptor;

    FabricModJarMetadata(ModContainer modContainer) {
        this.modContainer = modContainer;
    }

    @Override
    public String name() {
        if (name != null) return name;

        String id = modContainer.getMetadata().getId();
        StringJoiner joiner = new StringJoiner(".");
        int start = 0;
        int dashIndex;

        while (start < id.length() && (dashIndex = id.indexOf('-', start)) >= 0) {
            String part = id.substring(start, dashIndex);

            if (part.isEmpty()) {
                joiner.add("$");
            } else if (!Character.isJavaIdentifierStart(part.charAt(0))) {
                joiner.add("$" + part);
            } else {
                joiner.add(part);
            }

            start = dashIndex + 1;
        }

        if (start < id.length()) {
            joiner.add(id.substring(start));
        }

        name = joiner.toString();
        Logging.LOGGER.debug(Logging.MODULES, "Converted mod ID {} into module name {}", id, name);
        return name;
    }

    @Override
    public String version() {
        return modContainer.getMetadata().getVersion().getFriendlyString();
    }

    @Override
    public ModuleDescriptor descriptor() {
        if (descriptor != null) return descriptor;
        return descriptor = ModuleDescriptor.newAutomaticModule(name())
            .version(version())
            .build();
    }
}
