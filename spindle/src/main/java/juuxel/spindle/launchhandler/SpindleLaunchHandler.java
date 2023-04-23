/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.launchhandler;

import cpw.mods.modlauncher.api.ILaunchHandlerService;
import cpw.mods.modlauncher.api.ITransformingClassLoaderBuilder;
import cpw.mods.modlauncher.api.ServiceRunner;
import juuxel.spindle.Spindle;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.util.Arguments;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public class SpindleLaunchHandler implements ILaunchHandlerService {
    @Override
    public String name() {
        return "spindle";
    }

    public @Nullable EnvType side() {
        return null;
    }

    @SuppressWarnings("removal") // you know, we have to override this...
    @Override
    public void configureTransformationClassLoader(ITransformingClassLoaderBuilder builder) {
    }

    @Override
    public ServiceRunner launchService(String[] arguments, ModuleLayer gameLayer) {
        return () -> {
            Spindle.INSTANCE.getLauncher().setTargetClassLoader(Thread.currentThread().getContextClassLoader());

            Module minecraft = gameLayer.findModule("minecraft").orElseThrow();
            GameProvider provider = Spindle.INSTANCE.getGameProvider();
            String entrypoint = provider.getEntrypoint();
            String[] args = processArgs(provider, arguments);
            Class.forName(minecraft, entrypoint)
                .getMethod("main", String[].class)
                .invoke(null, (Object) args);
        };
    }

    private static String[] processArgs(GameProvider provider, String[] args) {
        try {
            Arguments arguments = new Arguments();
            arguments.parse(args);

            // In case we're dealing with MinecraftGameProvider (highly likely!),
            // modify the args array to include some defaults.
            Method processArgumentMap = provider.getClass()
                .getDeclaredMethod("processArgumentMap", Arguments.class, EnvType.class);
            processArgumentMap.setAccessible(true);
            processArgumentMap.invoke(null, arguments, Spindle.INSTANCE.getEnvType());

            return arguments.toArray();
        } catch (ReflectiveOperationException e) {
            return args;
        }
    }
}
