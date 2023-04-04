package juuxel.spindle.classpath;

public interface Classpath {
    ClassLoader getTargetClassLoader();
    boolean isClassLoaded(String name);
    Class<?> loadIntoTarget(String name) throws ClassNotFoundException;
}
