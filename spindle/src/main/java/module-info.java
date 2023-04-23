module io.github.juuxel.spindle {
    // Loading
    requires net.fabricmc.loader;
    requires cpw.mods.modlauncher;
    requires cpw.mods.securejarhandler;

    // ASM
    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires static org.spongepowered.mixin; // static requires because the fabric fork yeets the module-info????

    // Random libraries
    requires jopt.simple;
    requires org.apache.logging.log4j;

    // Compile-only libraries
    requires static org.jetbrains.annotations;

    // Services
    provides cpw.mods.modlauncher.serviceapi.ILaunchPluginService
        with juuxel.spindle.SpindleLaunchPlugin;
    provides cpw.mods.modlauncher.api.ITransformationService
        with juuxel.spindle.SpindleTransformationService;
    provides cpw.mods.modlauncher.api.ILaunchHandlerService
        with juuxel.spindle.launchhandler.SpindleLaunchHandler,
            juuxel.spindle.launchhandler.SpindleClientLaunchHandler,
            juuxel.spindle.launchhandler.SpindleDedicatedServerLaunchHandler;

    uses net.fabricmc.loader.impl.game.GameProvider;
}
