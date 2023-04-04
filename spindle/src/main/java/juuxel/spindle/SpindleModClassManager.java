package juuxel.spindle;

import cpw.mods.jarhandling.SecureJar;
import juuxel.spindle.classpath.Classpath;
import juuxel.spindle.util.Logging;
import juuxel.spindle.util.NoClassLoader;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.UrlConversionException;
import net.fabricmc.loader.impl.util.UrlUtil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class SpindleModClassManager {
    private final List<Path> codeSources = new ArrayList<>();
    private final Map<Path, String[]> allowedPrefixes = new HashMap<>();
    private final ModifiableUrlClassLoader classLoader = new ModifiableUrlClassLoader(new URL[0]);

    private static Object getFileKey(Path path) {
        try {
            return Files.readAttributes(path, BasicFileAttributes.class).fileKey();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    List<SecureJar> getCodeSources(Classpath classpath) {
        // A set of file keys for checking if a module has already been added to a module layer
        // on the provided classpath. File keys are used because union URIs used by SecureJarHandler are
        // different from file URIs.
        Set<Object> moduleFileKeys = classpath.codeSources()
            .map(Path::of)
            .filter(Files::exists)
            .map(SpindleModClassManager::getFileKey)
            .collect(Collectors.toCollection(HashSet::new));

        List<SecureJar> jars = new ArrayList<>(codeSources.size());

        for (Path source : codeSources) {
            Object fileKey = getFileKey(source);
            if (moduleFileKeys.contains(fileKey)) {
                Logging.LOGGER.debug(Logging.MODULES, "Skipping {} because it is already provided", source);
                continue;
            } else {
                Logging.LOGGER.debug(Logging.MODULES, "Adding code source {} to module layer GAME", source);
                moduleFileKeys.add(fileKey);
            }

            BiPredicate<String, String> pathFilter = (path, basePath) -> isAllowedToLoad(source, path.replace('/', '.'));
            jars.add(SecureJar.from(pathFilter, source));
        }

        return jars;
    }

    void addCodeSource(Path path) {
        try {
            codeSources.add(path);
            classLoader.addURL(path.toUri().toURL());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Could not convert path " + path + " to URL", e);
        }
    }

    void setAllowedPrefixes(Path path, String[] prefixes) {
        allowedPrefixes.put(LoaderUtil.normalizeExistingPath(path), prefixes);
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

    Map.Entry<Set<String>, Supplier<Function<String, Optional<URL>>>> getClassesLocator() {
        Set<String> allPrefixes = allowedPrefixes.values()
            .stream()
            .flatMap(Arrays::stream)
            .collect(Collectors.toSet());

        Supplier<Function<String, Optional<URL>>> locator = () -> className -> {
            String classFile = className.replace('.', '/') + ".class";
            URL url = classLoader.getResource(classFile);
            if (url == null) return Optional.empty();

            // Check prefixes
            try {
                Path path = LoaderUtil.normalizeExistingPath(UrlUtil.getCodeSource(url, classFile));
                if (!isAllowedToLoad(path, className)) {
                    return Optional.empty();
                }
            } catch (UrlConversionException e) {
                // ignored
            }

            return Optional.of(url);
        };
        return Map.entry(allPrefixes, locator);
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
