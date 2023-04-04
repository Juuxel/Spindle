package juuxel.spindle;

import cpw.mods.jarhandling.SecureJar;
import juuxel.spindle.util.NoClassLoader;
import net.fabricmc.loader.impl.util.LoaderUtil;
import net.fabricmc.loader.impl.util.UrlConversionException;
import net.fabricmc.loader.impl.util.UrlUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

    List<SecureJar> getCodeSources() {
        List<SecureJar> jars = new ArrayList<>(codeSources.size());

        for (Path source : codeSources) {
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
