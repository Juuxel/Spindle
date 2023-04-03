module io.github.juuxel.spindle.dev {
    requires cpw.mods.modlauncher;
    requires com.google.gson;

    provides cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService
        with juuxel.spindle.dev.SpindleDevDiscoveryService;
}
