package juuxel.spindle;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.fabricmc.loader.impl.transformer.FabricTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.EnumSet;

public final class SpindleLaunchPlugin implements ILaunchPluginService {
    @Override
    public String name() {
        return "spindle";
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        return isEmpty ? EnumSet.noneOf(Phase.class) : EnumSet.of(Phase.BEFORE);
    }

    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
        Spindle spindle = Spindle.INSTANCE;
        // TODO: where does this go
        //  spindle.getGameProvider().getEntrypointTransformer().transform()

        // Write out original class
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        byte[] bytes = writer.toByteArray();

        // Transform
        String dottyName = classNode.name.replace('/', '.');
        bytes = FabricTransformer.transform(spindle.isDevelopment(), spindle.getEnvType(), dottyName, bytes);

        // Read back new bytes
        ClassReader reader = new ClassReader(bytes);
        reader.accept(classNode, 0);

        return false;
    }
}
