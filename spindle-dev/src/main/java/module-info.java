module io.github.juuxel.spindle.dev {
    requires cpw.mods.modlauncher;
    requires com.google.gson;
    requires jdk.zipfs;

    provides cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService
        with juuxel.spindle.dev.SpindleDevDiscoveryService;
}
