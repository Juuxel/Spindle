/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import juuxel.spindle.util.ClassClearer;
import net.fabricmc.loader.impl.transformer.FabricTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
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

        // Try to apply entrypoint patch
        String dottyName = getDottyName(classNode);
        byte[] bytes = spindle.getGameProvider()
            .getEntrypointTransformer()
            .transform(getDottyName(classNode));

        if (bytes == null) {
            // Write out original class
            bytes = writeNode(classNode);
        } else {
            FindNameVisitor visitor = new FindNameVisitor();
            new ClassReader(bytes)
                .accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            dottyName = visitor.name;
        }

        // Transform
        bytes = FabricTransformer.transform(spindle.isDevelopment(), spindle.getEnvType(), dottyName, bytes);

        // Read back new bytes
        insertBytesIntoNode(classNode, bytes);

        return false;
    }

    private static String getDottyName(ClassNode node) {
        return node.name.replace('/', '.');
    }

    private static byte[] writeNode(ClassNode node) {
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void insertBytesIntoNode(ClassNode node, byte[] bytes) {
        clearClassNode(node);
        ClassReader reader = new ClassReader(bytes);
        reader.accept(node, 0);
    }

    private static void clearClassNode(ClassNode node) {
        ClassClearer.forClass(ClassNode.class).clear(node);
    }

    private static final class FindNameVisitor extends ClassVisitor {
        String name;

        FindNameVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.name = name;
        }
    }
}
