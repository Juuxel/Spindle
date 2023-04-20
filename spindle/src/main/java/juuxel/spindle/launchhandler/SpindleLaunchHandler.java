/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.launchhandler;

import cpw.mods.cl.ModuleClassLoader;
import cpw.mods.modlauncher.api.ILaunchHandlerService;
import cpw.mods.modlauncher.api.ITransformingClassLoaderBuilder;
import cpw.mods.modlauncher.api.ServiceRunner;
import juuxel.spindle.Spindle;

import java.util.Optional;

public class SpindleLaunchHandler implements ILaunchHandlerService {
    @Override
    public String name() {
        return "spindle";
    }

    public Optional<String> side() {
        return Optional.empty();
    }

    @SuppressWarnings("removal") // you know, we have to override this...
    @Override
    public void configureTransformationClassLoader(ITransformingClassLoaderBuilder builder) {
    }

    @Override
    public ServiceRunner launchService(String[] arguments, ModuleLayer gameLayer) {
        return () -> {
            ClassLoader cl = new ModuleClassLoader("Spindle", gameLayer.configuration(), gameLayer.parents());
            Spindle.INSTANCE.getGameProvider().launch(cl);
        };
    }
}
