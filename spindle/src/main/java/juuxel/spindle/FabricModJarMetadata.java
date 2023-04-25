/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import juuxel.spindle.util.Logging;
import juuxel.spindle.util.ModuleNames;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.Nullable;

import java.lang.module.ModuleDescriptor;
import java.util.jar.Attributes;

final class FabricModJarMetadata implements JarMetadata {
    private static final String CUSTOM_MODULE_NAME_KEY = "spindle:module_name";

    private final ModContainer modContainer;
    private final SecureJar jar;
    private String name;
    private ModuleDescriptor descriptor;

    FabricModJarMetadata(ModContainer modContainer, SecureJar jar) {
        this.modContainer = modContainer;
        this.jar = jar;
    }

    @Override
    public String name() {
        if (name != null) return name;

        String id = modContainer.getMetadata().getId();

        // 1. Try custom value.
        @Nullable var customValue = modContainer.getMetadata().getCustomValue(CUSTOM_MODULE_NAME_KEY);
        if (customValue != null) {
            name = customValue.getAsString();
            Logging.LOGGER.debug(Logging.MODULES, "Using module name {} from custom value for mod {}", name, id);
            return name;
        }

        // 2. Try manually specified automatic module name.
        @Nullable var automaticName = getAutomaticModuleName(jar);
        if (automaticName != null) {
            name = automaticName;
            Logging.LOGGER.debug(Logging.MODULES, "Using automatic module name {} for mod {}", name, id);
            return name;
        }

        // 3. Convert mod ID into module name.
        name = ModuleNames.fromModId(id);
        Logging.LOGGER.debug(Logging.MODULES, "Converted mod ID {} into module name {}", id, name);
        return name;
    }

    private static @Nullable String getAutomaticModuleName(SecureJar jar) {
        Attributes attributes = jar.moduleDataProvider().getManifest().getMainAttributes();
        return attributes.getValue("Automatic-Module-Name");
    }

    @Override
    public String version() {
        return modContainer.getMetadata().getVersion().getFriendlyString();
    }

    @Override
    public ModuleDescriptor descriptor() {
        if (descriptor != null) return descriptor;
        ModuleDescriptor.Builder builder = ModuleDescriptor.newAutomaticModule(name())
            .version(version())
            .packages(jar.getPackages());

        jar.getProviders().stream()
            .filter(provider -> !provider.providers().isEmpty())
            .forEach(provider -> builder.provides(provider.serviceName(), provider.providers()));

        return descriptor = builder.build();
    }
}
