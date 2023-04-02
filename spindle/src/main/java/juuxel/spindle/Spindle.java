package juuxel.spindle;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.ITransformationService;
import juuxel.spindle.util.LogCategories;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.entrypoint.EntrypointUtils;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.util.log.Log;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.jar.Manifest;

final class Spindle {
    static final Spindle INSTANCE = new Spindle();

    private final FabricLauncher launcher = new Launcher();
    private final SpindleModClassManager classManager = new SpindleModClassManager();
    private EnvType envType;
    private GameProvider gameProvider;
    private FabricLoaderImpl loader;
    private @Nullable ModuleClasspath moduleClasspath;

    private Spindle() {
    }

    private static String getSpindleVersion() {
        return Spindle.class.getModule()
            .getDescriptor()
            .rawVersion()
            .orElse("[unknown version]");
    }

    void init(IEnvironment environment) {
        // Set up Spindle
        moduleClasspath = environment.findModuleLayerManager()
            .flatMap(manager -> ModuleClasspath.find(manager,
                IModuleLayerManager.Layer.SERVICE, IModuleLayerManager.Layer.BOOT))
            .orElse(null);

        // Set up Loader
        envType = determineEnvType();
        gameProvider = determineGameProvider();
        Log.finishBuiltinConfig();
        Log.info(LogCategories.GENERAL, "Loading %s %s with Fabric Loader %s via Spindle %s",
            gameProvider.getGameName(),
            gameProvider.getRawGameVersion(),
            FabricLoaderImpl.VERSION,
            getSpindleVersion());
    }

    void createGameModuleClasspath(IModuleLayerManager layerManager) {
        moduleClasspath = ModuleClasspath.find(layerManager, IModuleLayerManager.Layer.GAME)
            .orElseThrow(() -> new RuntimeException("Could not find GAME module layer!"));
    }

    ITransformationService.Resource loadMods() {
        // 1. Set up game provider
        // Note: MinecraftGameProvider.initialize requires
        gameProvider.initialize(launcher);
        loader = FabricLoaderImpl.INSTANCE;
        loader.setGameProvider(gameProvider);

        // 2. Load mods
        loader.load();
        loader.freeze();
        loader.loadAccessWideners();

        // TODO: init mixin

        gameProvider.unlockClassPath(launcher);

        // 3. Execute preLaunch
        try {
            EntrypointUtils.invoke("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);
        } catch (Exception e) {
            throw FormattedException.ofLocalized("exception.initializerFailure", e);
        }

        // 4. Build resource from mod and classpath jars
        return new ITransformationService.Resource(IModuleLayerManager.Layer.GAME, classManager.getCodeSources());
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

            // TODO: args
            if (provider.locateGame(launcher, null)) {
                return provider;
            } else {
                badProviders.add(provider);
            }
        }

        // Could not find game provider, report an error
        StringBuilder error = new StringBuilder("Could not find game provider. ");

        if (badProviders.isEmpty()) {
            error.append("There were enabled game providers.");
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

        Log.error(LogCategories.LOADING, error.toString());
        throw new RuntimeException(error.toString());
    }

    public GameProvider getGameProvider() {
        GameProvider provider = gameProvider;
        if (provider == null) throw new IllegalStateException("Game provider not available");
        return provider;
    }

    public boolean isDevelopment() {
        // TODO: implement
        throw new UnsupportedOperationException();
    }

    public EnvType getEnvType() {
        return envType;
    }

    private final class Launcher extends FabricLauncherBase {
        @Override
        public void addToClassPath(Path path, String... allowedPrefixes) {
            classManager.addCodeSource(path);
            classManager.setAllowedPrefixes(path, allowedPrefixes);
        }

        @Override
        public void setAllowedPrefixes(Path path, String... prefixes) {
            classManager.setAllowedPrefixes(path, prefixes);
        }

        @Override
        public void setValidParentClassPath(Collection<Path> paths) {
            // TODO: Do we need this?
        }

        @Override
        public EnvType getEnvironmentType() {
            return getEnvType();
        }

        @Override
        public boolean isClassLoaded(String name) {
            return moduleClasspath().isClassLoaded(name);
        }

        @Override
        public Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
            return moduleClasspath().loadIntoTarget(name);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            return getTargetClassLoader().getResourceAsStream(name);
        }

        @Override
        public ClassLoader getTargetClassLoader() {
            return moduleClasspath().getTargetClassLoader();
        }

        @Override
        public byte[] getClassByteArray(String name, boolean runTransformers) throws IOException {
            return new byte[0];
        }

        @Override
        public Manifest getManifest(Path originPath) {
            return null;
        }

        @Override
        public boolean isDevelopment() {
            return Spindle.this.isDevelopment();
        }

        @Override
        public String getEntrypoint() {
            return getGameProvider().getEntrypoint();
        }

        @Override
        public String getTargetNamespace() {
            return isDevelopment() ? "named" : "intermediary";
        }

        @Override
        public List<Path> getClassPath() {
            return null;
        }

        private ModuleClasspath moduleClasspath() {
            ModuleClasspath mc = moduleClasspath;
            if (mc == null) throw new IllegalStateException("Module classpath not available");
            return mc;
        }
    }
}
