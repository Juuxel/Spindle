package juuxel.spindle;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.TypesafeMap;
import juuxel.spindle.classpath.Classpath;
import juuxel.spindle.classpath.LazyClasspath;
import juuxel.spindle.classpath.ModuleClasspath;
import juuxel.spindle.util.Logging;
import juuxel.spindle.util.TypesafeMapWrapper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.entrypoint.EntrypointUtils;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.log.Log;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
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
    private @Nullable Classpath gameClasspath;
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
        gameClasspath = new LazyClasspath(
            () -> ModuleClasspath.findThrowing(layerManager, IModuleLayerManager.Layer.GAME),
            ModuleClasspath.find(layerManager, IModuleLayerManager.Layer.PLUGIN)
                .orElseThrow(() -> new RuntimeException("Could not find PLUGIN module layer!"))
        );
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

        // 3. Initialise mixin
        initMixin();

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

    private final class Launcher extends FabricLauncherBase {
        Launcher() {
            TypesafeMap blackboard = cpw.mods.modlauncher.Launcher.INSTANCE.blackboard();
            setProperties(new TypesafeMapWrapper(blackboard));
        }

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
            return gameClasspath().isClassLoaded(name);
        }

        @Override
        public Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
            return gameClasspath().loadIntoTarget(name);
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            return getTargetClassLoader().getResourceAsStream(name);
        }

        @Override
        public ClassLoader getTargetClassLoader() {
            return gameClasspath().getTargetClassLoader();
        }

        @Override
        public byte[] getClassByteArray(String name, boolean runTransformers) {
            // only used by MixinServiceKnot as of 0.14.19
            throw new UnsupportedOperationException("getClassByteArray");
        }

        @Override
        public Manifest getManifest(Path originPath) {
            // not used at all as of 0.14.19
            throw new UnsupportedOperationException("getManifest");
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
            return launcherClasspath;
        }

        @Override
        public boolean useFabricMixinServices() {
            return false;
        }

        private Classpath gameClasspath() {
            Classpath cp = gameClasspath;
            if (cp == null) throw new IllegalStateException("Game classpath not available");
            return cp;
        }
    }
}
