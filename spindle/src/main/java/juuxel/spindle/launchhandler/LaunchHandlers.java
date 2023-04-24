/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.launchhandler;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ILaunchHandlerService;
import juuxel.spindle.util.Logging;
import net.fabricmc.api.EnvType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Utilities for working with {@linkplain ILaunchHandlerService launch handler services}.
 */
public final class LaunchHandlers {
    private static final String SIDE_SYSTEM_PROPERTY = "fabric.side";
    private static final String CLIENT_SIDE = "client";
    private static final String SERVER_SIDE = "server";

    /**
     * Tries to determine the environment type from the ModLauncher environment.
     *
     * <p>Specifically, it checks these in order:
     * <ol>
     *     <li>system property {@value #SIDE_SYSTEM_PROPERTY}, either {@value #CLIENT_SIDE} or {@value #SERVER_SIDE}
     *     <li>if the launch handler is a {@link SpindleLaunchHandler}, its {@linkplain SpindleLaunchHandler#side() side}
     *     <li>the distribution of FML launch handlers
     *     <li>{@code client} or {@code server} in the launch target name (case-insensitive)
     * </ol>
     *
     * <p>If the environment type cannot be detected, this method fails.
     *
     * @param environment the system environment
     * @return the determined environment type
     */
    public static EnvType determineEnvType(IEnvironment environment) {
        String side = System.getProperty(SIDE_SYSTEM_PROPERTY);
        if (side != null) return parseEnv(side);

        // If the system property is not specified, let's try figuring it out based on the launch target.
        @Nullable String launchTarget = environment.getProperty(IEnvironment.Keys.LAUNCHTARGET.get())
            .orElse(null);

        if (launchTarget != null) {
            @Nullable ILaunchHandlerService launchHandler = environment.findLaunchHandler(launchTarget)
                .orElse(null);

            if (launchHandler instanceof SpindleLaunchHandler spindleHandler) {
                // Some Spindle launch handlers provide the side.
                @Nullable EnvType env = spindleHandler.side();
                if (env != null) {
                    Logging.LOGGER.debug(Logging.LOADING, "Using Spindle launch target's environment {}", env);
                    return env;
                }
            }

            // Try figuring out the launch handler based on FML's launch handlers.
            if (launchHandler != null) {
                @Nullable EnvType env = LaunchHandlers.getFmlEnvType(launchHandler);
                if (env != null) {
                    Logging.LOGGER.debug(Logging.LOADING, "Using FML launch target's environment {}", env);
                    return env;
                }
            }

            launchTarget = launchTarget.toLowerCase(Locale.ROOT);
            if (launchTarget.contains("client")) {
                Logging.LOGGER.debug(Logging.LOADING, "Using launch target name-based environment: client");
                return EnvType.CLIENT;
            } else if (launchTarget.contains("server")) {
                Logging.LOGGER.debug(Logging.LOADING, "Using launch target name-based environment: server");
                return EnvType.SERVER;
            }
        }

        // If it's still missing, crash.
        throw new RuntimeException("Please specify system property fabric.side (either 'client' or 'server')");
    }

    private static EnvType parseEnv(String side) {
        return switch (side.toLowerCase(Locale.ROOT)) {
            case CLIENT_SIDE -> EnvType.CLIENT;
            case SERVER_SIDE -> EnvType.SERVER;
            default -> throw new IllegalArgumentException("Unknown side '%s', please specify 'client' or 'server'".formatted(side));
        };
    }

    private static @Nullable EnvType getFmlEnvType(ILaunchHandlerService service) {
        try {
            Method getDist = service.getClass().getMethod("getDist");
            Enum<?> dist = (Enum<?>) getDist.invoke(service);
            String name = dist.name().toLowerCase(Locale.ROOT);

            if (name.contains("client")) {
                return EnvType.CLIENT;
            } else if (name.contains("server")) {
                return EnvType.SERVER;
            }

            return null;
        } catch (ReflectiveOperationException e) {
            Logging.LOGGER.debug(Logging.LOADING, "Could not find env type for launch handler service {} - it is probably not FML's", service, e);
            return null;
        }
    }
}
