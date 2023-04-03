package juuxel.spindle;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public final class SpindleTransformationService implements ITransformationService {
    private static final String[] ARGUMENT_KEYS = new String[] {
        "accessToken",
        "version",
        "versionType",
        "gameDir",
        "assetsDir",
        "server",
        "port",
        "session",
        "username",
        "width",
        "height",
        "fabric.gameVersion",
        "fabric.addMods",
    };

    static {
        Spindle.INSTANCE.preInit();
    }

    private final Spindle spindle = Spindle.INSTANCE;
    private Map<String, OptionSpec<String>> args;

    @Override
    public @NotNull String name() {
        return "spindle";
    }

    @Override
    public void initialize(IEnvironment environment) {
        spindle.init(environment);
    }

    @Override
    public List<Resource> completeScan(IModuleLayerManager layerManager) {
        spindle.createGameModuleClasspath(layerManager);
        return List.of(spindle.loadMods());
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
        // we aren't supposed to be incompatible with anything
    }

    @Override
    public void arguments(BiFunction<String, String, OptionSpecBuilder> argumentBuilder) {
        args = new HashMap<>();
        for (String key : ARGUMENT_KEYS) {
            args.put(key, argumentBuilder.apply(key, key).withOptionalArg().ofType(String.class));
        }
    }

    @Override
    public void argumentValues(OptionResult option) {
        List<String> argList = new ArrayList<>(args.size() * 2);
        args.forEach((key, spec) -> {
            String value = option.value(spec);

            if (value != null) {
                argList.add("--" + key);
                argList.add(value);
            }
        });
        spindle.setArgs(argList.toArray(String[]::new));
        args = null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public @NotNull List<ITransformer> transformers() {
        return List.of();
    }

//    @Override
//    public Map.Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> additionalClassesLocator() {
//        return spindle.getClassManager().getClassesLocator();
//    }
}
