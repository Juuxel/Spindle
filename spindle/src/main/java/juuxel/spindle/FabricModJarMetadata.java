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

import java.lang.module.ModuleDescriptor;

final class FabricModJarMetadata implements JarMetadata {
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
        name = ModuleNames.fromModId(id);
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
        ModuleDescriptor.Builder builder = ModuleDescriptor.newAutomaticModule(name())
            .version(version())
            .packages(jar.getPackages());

        jar.getProviders().stream()
            .filter(provider -> !provider.providers().isEmpty())
            .forEach(provider -> builder.provides(provider.serviceName(), provider.providers()));

        return descriptor = builder.build();
    }
}
