/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.bootstrap;

import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class SpindleBootstrapService implements ITransformerDiscoveryService {
    private static final String RESET_CACHE_PROPERTY = "spindle.resetCache";
    private static final String CACHE_DIR = ".spindle";
    private static final String JAR_STORE_DIR = "jars";

    @Override
    public List<NamedPath> candidates(Path gameDirectory) {
        try {
            boolean resetCache = Boolean.getBoolean(RESET_CACHE_PROPERTY);
            Path jarStore = gameDirectory.resolve(CACHE_DIR).resolve(JAR_STORE_DIR);
            Files.createDirectories(jarStore);

            URL url = SpindleBootstrapService.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();
            List<NamedPath> targets = new ArrayList<>();
            try (var zip = new ZipFile(getAsFile(url.toURI()))) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) continue;

                    String name = entry.getName();
                    if (!name.startsWith("META-INF/jars/")) continue;
                    name = name.substring(name.lastIndexOf('/') + 1);

                    Path target = jarStore.resolve(name);
                    if (resetCache || !Files.exists(target)) {
                        try (var in = zip.getInputStream(entry)) {
                            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    targets.add(new NamedPath(name, target));
                }
            }
            return List.copyOf(targets);
        } catch (Exception e) {
            throw e instanceof RuntimeException re ? re : new RuntimeException(e);
        }
    }

    private static File getAsFile(URI uri) {
        return switch (uri.getScheme()) {
            case "file" -> new File(uri);
            case "union" -> {
                String ssb = uri.getSchemeSpecificPart();
                String path = ssb.substring(0, ssb.lastIndexOf('#'));
                yield new File(URI.create("file:" + path));
            }
            default -> throw new IllegalArgumentException("Unknown URI scheme " + uri.getScheme() + " in " + uri);
        };
    }
}
