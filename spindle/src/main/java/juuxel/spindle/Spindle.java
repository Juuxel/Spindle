/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.ITransformationService;
import juuxel.spindle.classpath.Classpath;
import juuxel.spindle.classpath.LazyClasspath;
import juuxel.spindle.classpath.ModuleClasspath;
import juuxel.spindle.util.AdditionalResourcesClassLoader;
import juuxel.spindle.util.Logging;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.entrypoint.EntrypointUtils;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.log.Log;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;

final class Spindle {
    static final Spindle INSTANCE = new Spindle();

    private final SpindleFabricLauncher launcher = new SpindleFabricLauncher(this);
    final SpindleModClassManager classManager = new SpindleModClassManager();
    private EnvType envType;
    private GameProvider gameProvider;
    private FabricLoaderImpl loader;
    private Classpath pluginClasspath;
    @Nullable Classpath gameClasspath;
    private boolean isDevelopment;
    private final List<Path> launcherClasspath = new ArrayList<>();
    private String[] args;

    private Spindle() {
    }

    private static String getSpindleVersion() {
        return Spindle.class.getModule()
            .getDescriptor()
            .rawVersion()
            .orElse("[unknown version]");
    }

    void preInit() {
    }

    void init(IEnvironment environment) {
        // Set up Spindle
//        moduleClasspath = environment.findModuleLayerManager()
//            .flatMap(manager -> ModuleClasspath.find(manager,
//                IModuleLayerManager.Layer.SERVICE, IModuleLayerManager.Layer.BOOT))
//            .orElse(null);

        // Set up classpath for FabricLauncher.getClassPath
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            launcherClasspath.add(LoaderUtil.normalizePath(Path.of(entry)));
        }

        // Set up Loader
        envType = determineEnvType();
        gameProvider = determineGameProvider();
        Log.finishBuiltinConfig();
        Logging.LOGGER.info("Loading {} {} with Fabric Loader {} via Spindle {}",
            gameProvider.getGameName(),
            gameProvider.getRawGameVersion(),
            FabricLoaderImpl.VERSION,
            getSpindleVersion());

        isDevelopment = Boolean.getBoolean("fabric.development");
    }

    void setArgs(String[] args) {
        this.args = args;
    }

    void createGameModuleClasspath(IModuleLayerManager layerManager) {
        pluginClasspath = ModuleClasspath.find(layerManager, IModuleLayerManager.Layer.PLUGIN)
            .orElseThrow(() -> new RuntimeException("Could not find PLUGIN module layer!"));
        gameClasspath = new LazyClasspath(
            () -> ModuleClasspath.findThrowing(layerManager, IModuleLayerManager.Layer.GAME),
            pluginClasspath
        );
    }

    List<Path> getLauncherClasspath() {
        return Collections.unmodifiableList(launcherClasspath);
    }

    ITransformationService.Resource loadMods() {
        // 1. Set up game provider
        gameProvider.initialize(launcher);
        loader = FabricLoaderImpl.INSTANCE;
        loader.setGameProvider(gameProvider);

        // 2. Load mods

        // Forge compat: FML isolates the class loaders for classloading,
        // but uses the system class loader for discovering mods.
        // We can follow suit by introducing a rather hacky class loader that
        // only reads resources from the system class loader.
        ClassLoader discoveryClassLoader = new AdditionalResourcesClassLoader(launcher.getTargetClassLoader(),
            List.of(ClassLoader.getSystemClassLoader()));
        launcher.targetClassLoader = discoveryClassLoader;
        loader.load();
        loader.freeze();
        loader.loadAccessWideners();
        // Let's throw away that hacky class loader. I won't miss you.
        launcher.targetClassLoader = null;

        // 3. Initialise mixin
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // We need the hacky discovering class loader for loading mixin configs.
            Thread.currentThread().setContextClassLoader(discoveryClassLoader);
            initMixin();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        // 4. Unlock game provider classpath (really, this doesn't do much for us
        // but Knot does it, so let's follow the logic)
        gameProvider.unlockClassPath(launcher);

        // 5. Execute preLaunch
        try {
            EntrypointUtils.invoke("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);
        } catch (Exception e) {
            throw FormattedException.ofLocalized("exception.initializerFailure", e);
        }

        // 6. Build resource from mod and classpath jars
        return new ITransformationService.Resource(IModuleLayerManager.Layer.GAME,
            classManager.getCodeSources(pluginClasspath));
    }

    private static EnvType determineEnvType() {
        String side = System.getProperty("fabric.side");
        if (side == null) throw new NullPointerException("Please specify system property fabric.side (either 'client' or 'server')");

        return switch (side.toLowerCase(Locale.ROOT)) {
            case "client" -> EnvType.CLIENT;
            case "server" -> EnvType.SERVER;
            default -> throw new RuntimeException("Unknown side '%s', please specify 'client' or 'server'".formatted(side));
        };
    }

    private GameProvider determineGameProvider() {
        List<GameProvider> badProviders = new ArrayList<>();

        for (GameProvider provider : ServiceLoader.load(GameProvider.class)) {
            if (!provider.isEnabled()) continue;

            if (provider.locateGame(launcher, args)) {
                return provider;
            } else {
                badProviders.add(provider);
            }
        }

        // Could not find game provider, report an error
        StringBuilder error = new StringBuilder("Could not find game provider. ");

        if (badProviders.isEmpty()) {
            error.append("There were no enabled game providers.");
        } else {
            error.append("I tried these game providers:");

            for (GameProvider provider : badProviders) {
                error.append("\n- ")
                    .append(provider.getGameName())
                    .append(" (")
                    .append(provider.getClass().getName())
                    .append(')');
            }
        }

        Logging.LOGGER.error(Logging.LOADING, error.toString());
        throw new RuntimeException(error.toString());
    }

    private void initMixin() {
        // TODO: Set up dev remapping?
        for (ModContainerImpl mod : loader.getModsInternal()) {
            for (String config : mod.getMetadata().getMixinConfigs(envType)) {
                try {
                    Mixins.addConfiguration(config);
                } catch (Exception e) {
                    throw new RuntimeException(
                        "Could not add mixin config %s from mod %s"
                            .formatted(config, mod.getMetadata().getId()),
                        e
                    );
                }
            }
        }
    }

    public GameProvider getGameProvider() {
        GameProvider provider = gameProvider;
        if (provider == null) throw new IllegalStateException("Game provider not available");
        return provider;
    }

    public boolean isDevelopment() {
        return isDevelopment;
    }

    public EnvType getEnvType() {
        return envType;
    }
}
