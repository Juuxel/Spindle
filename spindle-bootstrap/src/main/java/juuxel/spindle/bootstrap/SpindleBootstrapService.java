/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.bootstrap;

import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService;

import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            URI uri = new URI("jar:" + url.toURI());
            List<Path> targets = new ArrayList<>();
            try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                Path jars = fs.getPath("META-INF", "jars");
                try (DirectoryStream<Path> dir = Files.newDirectoryStream(jars)) {
                    for (Path path : dir) {
                        Path target = jarStore.resolve(path.getFileName().toString());
                        if (resetCache || !Files.exists(target)) {
                            Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                            targets.add(target);
                        }
                    }
                }
            }
            return List.of(new NamedPath("spindle", targets.toArray(new Path[0])));
        } catch (Exception e) {
            throw e instanceof RuntimeException re ? re : new RuntimeException(e);
        }
    }
}
