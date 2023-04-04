package juuxel.spindle.util;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class AdditionalResourcesClassLoader extends ClassLoader {
    private final List<ClassLoader> sources;

    public AdditionalResourcesClassLoader(ClassLoader parent, List<ClassLoader> sources) {
        super(parent);
        this.sources = sources;
    }

    @Nullable
    @Override
    public URL getResource(String name) {
        for (ClassLoader source : sources) {
            URL url = source.getResource(name);
            if (url != null) return url;
        }

        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        List<URL> list = new ArrayList<>();

        for (ClassLoader source : sources) {
            for (URL url : (Iterable<URL>) source.getResources(name)::asIterator) {
                list.add(url);
            }
        }

        for (URL url : (Iterable<URL>) super.getResources(name)::asIterator) {
            list.add(url);
        }

        return Collections.enumeration(list);
    }

    static {
        registerAsParallelCapable();
    }
}
