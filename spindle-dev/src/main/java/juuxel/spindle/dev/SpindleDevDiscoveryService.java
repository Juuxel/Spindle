package juuxel.spindle.dev;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SpindleDevDiscoveryService implements ITransformerDiscoveryService {
    @Override
    public List<NamedPath> candidates(Path gameDirectory) {
        List<NamedPath> result = new ArrayList<>();
        String[] classpath = System.getProperty("java.class.path").split(File.pathSeparator);

        // Round 1: Find Loader and collect its dependencies
        List<String> libraries = new ArrayList<>();
        for (String cpEntry : classpath) {
            Path path = Path.of(cpEntry);
            URI jarUri = URI.create("jar:" + path.toUri());

            try (FileSystem fs = FileSystems.newFileSystem(jarUri, Map.of("create", false))) {
                Path installerPath = fs.getPath("fabric-installer.json");
                if (Files.exists(installerPath)) {
                    result.add(new NamedPath("fabricloader", path));

                    try (Reader r = Files.newBufferedReader(installerPath)) {
                        JsonObject json = new Gson().fromJson(r, JsonObject.class);
                        JsonArray commonLibraries = json.getAsJsonObject("libraries")
                            .getAsJsonArray("common");

                        for (JsonElement library : commonLibraries) {
                            String name = library.getAsJsonObject()
                                .getAsJsonPrimitive("name")
                                .getAsString();
                            libraries.add(name);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException("Could not read fabric-installer.json", e);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Could not access jar", e);
            }
        }

        if (result.isEmpty()) {
            System.err.println("[Spindle/DevDiscovery] Could not locate fabric-installer.json in any classpath file");
            return List.of();
        }

        // Round 2: Discover loader deps
        List<String> libraryFileNames = libraries.stream()
            .map(library -> {
                // TODO: what if a version is upgraded by Gradle?
                //  Maybe a Pattern with a "hole" at the version would be better.
                String[] parts = library.split(":");
                if (parts.length > 3) {
                    return "%s-%s-%s.jar".formatted(parts[1], parts[2], parts[3]);
                } else {
                    return "%s-%s.jar".formatted(parts[1], parts[2]);
                }
            })
            .toList();

        List<Path> libraryPaths = new ArrayList<>();

        for (String cpEntry : classpath) {
            Path path = Path.of(cpEntry);
            if (libraryFileNames.contains(path.getFileName().toString())) {
                libraryPaths.add(path);
            }
        }

        result.add(new NamedPath("fabricloaderdependencies", libraryPaths.toArray(Path[]::new)));
        return result;
    }
}
