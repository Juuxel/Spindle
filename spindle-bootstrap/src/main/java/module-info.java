module io.github.juuxel.spindle.bootstrap {
    requires cpw.mods.modlauncher;
    provides cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService
        with juuxel.spindle.bootstrap.SpindleBootstrapService;
}
