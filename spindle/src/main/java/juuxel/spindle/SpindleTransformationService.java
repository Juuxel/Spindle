package juuxel.spindle;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public final class SpindleTransformationService implements ITransformationService {
    private Spindle spindle;

    @Override
    public @NotNull String name() {
        return "spindle";
    }

    @Override
    public void initialize(IEnvironment environment) {
        spindle = Spindle.INSTANCE;
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
