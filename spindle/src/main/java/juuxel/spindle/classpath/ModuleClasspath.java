package juuxel.spindle.classpath;

import cpw.mods.modlauncher.api.IModuleLayerManager;
import juuxel.spindle.util.AccessibleClassLoader;
import juuxel.spindle.util.Logging;

import java.lang.module.Configuration;
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class ModuleClasspath implements Classpath {
    private final ModuleLayer layer;
    private final AccessibleClassLoader classLoader;

    private ModuleClasspath(ModuleLayer layer, IModuleLayerManager.Layer type) {
        this.layer = layer;

        Set<ClassLoader> cls = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Module module : layer.modules()) {
            cls.add(module.getClassLoader());
        }

        if (cls.isEmpty()) {
            throw new IllegalArgumentException("Module layer " + type.name() + " has no modules!");
        } else if (cls.size() > 1) {
            Logging.LOGGER.error(Logging.MODULES,
                "Module layer {} has more than one class loader: {}; choosing first one",
                type, cls);
        }

        classLoader = new AccessibleClassLoader("Spindle/" + type.name(), cls.iterator().next());
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

    public static Optional<ModuleClasspath> find(IModuleLayerManager layerManager, IModuleLayerManager.Layer... layers) {
        try {
            return Optional.of(findThrowing(layerManager, layers));
        } catch (ClasspathCreationException e) {
            Logging.LOGGER.warn(Logging.MODULES, e.getMessage());
            return Optional.empty();
        }
    }

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