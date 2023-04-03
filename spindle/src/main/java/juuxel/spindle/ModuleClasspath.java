package juuxel.spindle;

import cpw.mods.modlauncher.api.IModuleLayerManager;
import juuxel.spindle.util.AccessibleClassLoader;
import juuxel.spindle.util.Logging;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;

final class ModuleClasspath {
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

    static Optional<ModuleClasspath> find(IModuleLayerManager layerManager, IModuleLayerManager.Layer... layers) {
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
            Logging.LOGGER.warn(Logging.MODULES,
                "Could not find any module layer from alternatives " + Arrays.toString(layers));
            return Optional.empty();
        }

        return Optional.of(new ModuleClasspath(layer, type));
    }

    ClassLoader getTargetClassLoader() {
        return classLoader;
    }

    boolean isClassLoaded(String name) {
        return classLoader.findLoadedClassExt(name) != null;
    }

    Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
        Class<?> c = classLoader.findLoadedClassExt(name);

        if (c == null) {
            c = classLoader.loadClass(name, true);
        }

        return c;
    }
}
