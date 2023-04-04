/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: Just write it out manually, this really is not worth it.
final class ClassClearerImpl<T> implements ClassClearer<T> {
    private static final Map<Class<?>, ClassClearer<?>> clearers = new HashMap<>();

    private final List<MethodHandle> handlesToNull;
    private final List<MethodHandle> handlesToMinusOne;
    private final List<MethodHandle> handlesToClear;

    ClassClearerImpl(List<MethodHandle> handlesToNull, List<MethodHandle> handlesToMinusOne, List<MethodHandle> handlesToClear) {
        this.handlesToNull = handlesToNull;
        this.handlesToMinusOne = handlesToMinusOne;
        this.handlesToClear = handlesToClear;
    }

    @Override
    public void clear(T t) {
        try {
            for (MethodHandle handle : handlesToNull) {
                handle.invoke(t, null);
            }

            for (MethodHandle handle : handlesToMinusOne) {
                handle.invoke(t, -1);
            }

            for (MethodHandle handle : handlesToClear) {
                Collection<?> c = (Collection<?>) handle.invoke(t);
                if (c != null) c.clear();
            }
        } catch (Throwable e) {
            if (e instanceof Error err) {
                throw err;
            }

            throw new RuntimeException("Could not clear " + t, e);
        }
    }

    @SuppressWarnings("unchecked")
    static synchronized <T> ClassClearer<T> getClearer(Class<T> c) {
        if (clearers.containsKey(c)) {
            return (ClassClearer<T>) clearers.get(c);
        }

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        List<MethodHandle> handlesToNull = new ArrayList<>();
        List<MethodHandle> handlesToMinusOne = new ArrayList<>();
        List<MethodHandle> handlesToClear = new ArrayList<>();

        for (Field field : c.getFields()) {
            // We only want public fields.
            if ((field.getModifiers() & Modifier.PUBLIC) == 0) continue;

            // No static or final fields.
            if ((field.getModifiers() & (Modifier.STATIC | Modifier.FINAL)) != 0) continue;

            try {
                if (Collection.class.isAssignableFrom(field.getType())) {
                    MethodHandle handle = lookup.findGetter(c, field.getName(), field.getType());
                    handlesToClear.add(handle);
                } else if (int.class.equals(field.getType())) {
                    MethodHandle handle = lookup.findSetter(c, field.getName(), field.getType());
                    handlesToMinusOne.add(handle);
                } else {
                    MethodHandle handle = lookup.findSetter(c, field.getName(), field.getType());
                    handlesToNull.add(handle);
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Could not create class clearer", e);
            }
        }

        ClassClearer<T> result = new ClassClearerImpl<>(handlesToNull, handlesToMinusOne, handlesToClear);
        clearers.put(c, result);
        return result;
    }
}
