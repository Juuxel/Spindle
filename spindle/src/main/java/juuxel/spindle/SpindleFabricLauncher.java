/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.TypesafeMap;
import juuxel.spindle.classpath.Classpath;
import juuxel.spindle.util.TypesafeMapWrapper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.jar.Manifest;

final class SpindleFabricLauncher extends FabricLauncherBase {
    private final Spindle spindle;
    @Nullable ClassLoader targetClassLoader;

    SpindleFabricLauncher(Spindle spindle) {
        this.spindle = spindle;
        TypesafeMap blackboard = Launcher.INSTANCE.blackboard();
        setProperties(new TypesafeMapWrapper(blackboard));
    }

    @Override
    public void addToClassPath(Path path, String... allowedPrefixes) {
        spindle.classManager.addCodeSource(path);
        spindle.classManager.setAllowedPrefixes(path, allowedPrefixes);
    }

    @Override
    public void setAllowedPrefixes(Path path, String... prefixes) {
        spindle.classManager.setAllowedPrefixes(path, prefixes);
    }

    @Override
    public void setValidParentClassPath(Collection<Path> paths) {
        // no-op (we don't currently implement this behaviour)
    }

    @Override
    public EnvType getEnvironmentType() {
        return spindle.getEnvType();
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
        ClassLoader cl = targetClassLoader;
        if (cl != null) return cl;

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
        return spindle.isDevelopment();
    }

    @Override
    public String getEntrypoint() {
        return spindle.getGameProvider().getEntrypoint();
    }

    @Override
    public String getTargetNamespace() {
        return isDevelopment() ? "named" : "intermediary";
    }

    @Override
    public List<Path> getClassPath() {
        return spindle.getLauncherClasspath();
    }

    @Override
    public boolean useFabricMixinServices() {
        return false;
    }

    private Classpath gameClasspath() {
        Classpath cp = spindle.gameClasspath;
        if (cp == null) throw new IllegalStateException("Game classpath not available");
        return cp;
    }
}
