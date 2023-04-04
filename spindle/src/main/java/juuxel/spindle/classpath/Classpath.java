package juuxel.spindle.classpath;

import java.net.URI;
import java.util.stream.Stream;

public interface Classpath {
    Stream<URI> codeSources();
    ClassLoader getTargetClassLoader();
    boolean isClassLoaded(String name);
    Class<?> loadIntoTarget(String name) throws ClassNotFoundException;
}
