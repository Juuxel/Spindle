package juuxel.spindle.bootstrap;

import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService;

import java.nio.file.Path;
import java.util.List;

public final class SpindleBootstrapService implements ITransformerDiscoveryService {
    @Override
    public List<NamedPath> candidates(Path gameDirectory) {
        return null; // TODO: Implement
    }
}
