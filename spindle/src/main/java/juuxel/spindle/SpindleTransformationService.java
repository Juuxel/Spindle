/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
        "gameVersion",
        "addMods",
    };

    static {
        Spindle.INSTANCE.preInit();
    }

    private final Spindle spindle = Spindle.INSTANCE;
    private Map<String, OptionSpec<String>> args;

    @Override
    public @NotNull String name() {
        return "fabric"; // we need this for the CLI args
    }

    @Override
    public void initialize(IEnvironment environment) {
        spindle.init();
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
                argList.add("--" + name() + "." + key);
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
}
