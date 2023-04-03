package juuxel.spindle.util;

import cpw.mods.modlauncher.api.TypesafeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class TypesafeMapWrapper implements Map<String, Object> {
    private final TypesafeMap parent;

    public TypesafeMapWrapper(TypesafeMap parent) {
        this.parent = parent;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    private TypesafeMap.Key<Object> getKey(Object key) {
        return TypesafeMap.Key.getOrCreate(parent, Objects.toString(key), Object.class);
    }

    @Override
    public boolean containsKey(Object key) {
        return parent.get(getKey(key)).isPresent();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(Object key) {
        return parent.get(getKey(key)).orElse(null);
    }

    @Nullable
    @Override
    public Object put(String key, Object value) {
        parent.computeIfAbsent(getKey(key), k -> value);
        return null;
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ?> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Collection<Object> values() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException();
    }
}
