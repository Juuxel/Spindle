/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle;

import cpw.mods.jarhandling.SecureJar;
import juuxel.spindle.classpath.Classpath;
import juuxel.spindle.util.Fp;
import juuxel.spindle.util.Logging;
import juuxel.spindle.util.NoClassLoader;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.impl.util.LoaderUtil;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

final class SpindleModClassManager {
    private final List<Path> codeSources = new ArrayList<>();
    private final Map<Path, String[]> allowedPrefixes = new HashMap<>();
    // TODO: I don't think this is useful?
    private final ModifiableUrlClassLoader classLoader = new ModifiableUrlClassLoader(new URL[0]);

    private static Path getUnderlyingPath(URI uri) {
        if (uri.getScheme().equals("union")) {
            String ssb = uri.getSchemeSpecificPart();
            String path = ssb.substring(0, ssb.lastIndexOf('#'));
            uri = URI.create("file:" + path);
        }

        return LoaderUtil.normalizePath(Path.of(uri));
    }

    List<SecureJar> getCodeSources(FabricLoaderImpl loader, Classpath classpath) {
        // A set of paths for checking if a module has already been added to a module layer
        // on the provided classpath.
        Set<Path> pathsInUse = classpath.codeSources()
            .map(SpindleModClassManager::getUnderlyingPath)
            .collect(Collectors.toCollection(HashSet::new));

        Set<Path> remainingCodeSources = new LinkedHashSet<>(codeSources);
        List<SecureJar> jars = new ArrayList<>(codeSources.size());

        for (ModContainerImpl mod : loader.getModsInternal()) {
            List<Path> modPaths = new ArrayList<>(mod.getRootPaths().size());
            List<BiPredicate<String, String>> filters = new ArrayList<>();

            for (Path source : mod.getCodeSourcePaths()) {
                // just in case
                source = LoaderUtil.normalizeExistingPath(source);

                if (remainingCodeSources.remove(source) && checkPathAvailable(source, pathsInUse)) {
                    modPaths.add(source);
                    filters.add(createPathFilter(source));
                }
            }

            if (!modPaths.isEmpty()) {
                Logging.LOGGER.debug(Logging.MODULES, "Adding mod {} with sources {} to module layer GAME", mod.getMetadata().getId(), modPaths);
                // Not perfect, but this is good enough in practice.
                // Mods shouldn't have any filters in the first place.
                BiPredicate<String, String> pathFilter = Fp.any(filters);
                jars.add(SecureJar.from(jar -> new FabricModJarMetadata(mod, jar), pathFilter, modPaths.toArray(new Path[0])));
            }
        }

        for (Path source : remainingCodeSources) {
            if (checkPathAvailable(source, pathsInUse)) {
                Logging.LOGGER.debug(Logging.MODULES, "Adding code source {} to module layer GAME", source);
                jars.add(SecureJar.from(createPathFilter(source), source));
            }
        }

        return jars;
    }

    private static boolean checkPathAvailable(Path source, Set<Path> pathsInUse) {
        Path realPath = getUnderlyingPath(source.toUri());
        if (pathsInUse.contains(realPath)) {
            Logging.LOGGER.debug(Logging.MODULES, "Skipping {} because it is already provided", source);
            return false;
        } else {
            pathsInUse.add(realPath);
            return true;
        }
    }

    void addCodeSource(Path path) {
        path = LoaderUtil.normalizeExistingPath(path);

        try {
            codeSources.add(path);
            classLoader.addURL(path.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Could not convert path " + path + " to URL", e);
        }
    }

    void setAllowedPrefixes(Path path, String[] prefixes) {
        path = LoaderUtil.normalizeExistingPath(path);

        if (prefixes.length != 0) {
            allowedPrefixes.put(path, prefixes);
        } else {
            allowedPrefixes.remove(path);
        }
    }

    private BiPredicate<String, String> createPathFilter(Path codeSource) {
        return (path, basePath) -> isAllowedToLoad(codeSource, path.replace('/', '.'));
    }

    private boolean isAllowedToLoad(Path codeSource, String dottedResourceName) {
        // The root path and any jar metadata are always allowed.
        if (dottedResourceName.equals(".") || dottedResourceName.startsWith("META-INF.")) return true;

        String[] prefixes = allowedPrefixes.get(codeSource);
        if (prefixes == null) return true;

        for (String prefix : prefixes) {
            if (dottedResourceName.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static final class ModifiableUrlClassLoader extends URLClassLoader {
        public ModifiableUrlClassLoader(URL[] urls) {
            super(urls, new NoClassLoader());
        }

        @Override
        public void addURL(URL url) {
            super.addURL(url);
        }

        static {
            registerAsParallelCapable();
        }
    }
}
