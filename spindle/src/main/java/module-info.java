module io.github.juuxel.spindle {
    requires cpw.mods.modlauncher;
    requires cpw.mods.securejarhandler;
    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires static net.fabricmc.fabricloader; // not a real module
    requires static org.jetbrains.annotations;

    provides cpw.mods.modlauncher.api.ITransformationService
        with juuxel.spindle.SpindleTransformationService;
    provides cpw.mods.modlauncher.serviceapi.ILaunchPluginService
        with juuxel.spindle.SpindleLaunchPlugin;
    uses net.fabricmc.loader.impl.game.GameProvider;
}
