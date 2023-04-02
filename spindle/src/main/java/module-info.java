module io.github.juuxel.spindle {
    requires cpw.mods.modlauncher;
    requires cpw.mods.securejarhandler;
    requires static net.fabricmc.fabricloader; // not a real module
    requires static org.jetbrains.annotations;

    provides cpw.mods.modlauncher.api.ITransformationService
        with juuxel.spindle.SpindleTransformationService;
    uses net.fabricmc.loader.impl.game.GameProvider;
}
