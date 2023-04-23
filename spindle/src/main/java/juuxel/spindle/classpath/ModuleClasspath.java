/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.classpath;

import cpw.mods.cl.ModuleClassLoader;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import juuxel.spindle.util.AccessibleClassLoader;
import juuxel.spindle.util.Logging;

import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * A classpath created from a {@linkplain ModuleLayer module layer}.
 */
public final class ModuleClasspath implements Classpath {
    private final ModuleLayer layer;
    private final AccessibleClassLoader classLoader;

    private ModuleClasspath(ModuleLayer layer, IModuleLayerManager.Layer type) {
        this.layer = layer;

        ModuleClassLoader parentClassLoader = new ModuleClassLoader("Spindle/" + type.name(), layer.configuration(), layer.parents());
        parentClassLoader.setFallbackClassLoader(Thread.currentThread().getContextClassLoader());
        classLoader = new AccessibleClassLoader("Spindle/" + type.name(), parentClassLoader);
    }

    private static void visitResolvedModules(Configuration config, Consumer<ResolvedModule> sink) {
        Queue<Configuration> queue = new ArrayDeque<>();
        queue.offer(config);

        while ((config = queue.poll()) != null) {
            for (ResolvedModule module : config.modules()) {
                sink.accept(module);
            }

            for (Configuration parent : config.parents()) {
                queue.offer(parent);
            }
        }
    }

    /**
     * Tries to find a module layer and create a module classpath for it.
     *
     * @param layerManager the module layer manager
     * @param layers       the different module layer types to try
     * @return the module classpath, or empty if no module layer is found
     */
    public static Optional<ModuleClasspath> find(IModuleLayerManager layerManager, IModuleLayerManager.Layer... layers) {
        try {
            return Optional.of(findThrowing(layerManager, layers));
        } catch (ClasspathCreationException e) {
            Logging.LOGGER.warn(Logging.MODULES, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Tries to find a module layer and create a module classpath for it.
     *
     * @param layerManager the module layer manager
     * @param layers       the different module layer types to try
     * @return the module classpath
     * @throws ClasspathCreationException if no module layer was found
     */
    public static ModuleClasspath findThrowing(IModuleLayerManager layerManager, IModuleLayerManager.Layer... layers)
        throws ClasspathCreationException {
        ModuleLayer layer = null;
        IModuleLayerManager.Layer type = null;

        for (IModuleLayerManager.Layer layerType : layers) {
            ModuleLayer current = layerManager.getLayer(layerType).orElse(null);
            if (current != null) {
                layer = current;
                type = layerType;
                break;
            }
        }

        if (layer == null) {
            throw new ClasspathCreationException("Could not find any module layer from alternatives " + Arrays.toString(layers));
        }

        return new ModuleClasspath(layer, type);
    }

    @Override
    public Stream<URI> codeSources() {
        Stream.Builder<URI> builder = Stream.builder();
        visitResolvedModules(layer.configuration(), module -> module.reference().location().ifPresent(builder));
        return builder.build();
    }

    @Override
    public ClassLoader getTargetClassLoader() {
        return classLoader;
    }

    @Override
    public boolean isClassLoaded(String name) {
        return classLoader.findLoadedClassExt(name) != null;
    }

    @Override
    public Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
        Class<?> c = classLoader.findLoadedClassExt(name);

        if (c == null) {
            c = classLoader.loadClass(name, true);
        }

        return c;
    }
}
