package juuxel.spindle.util;

import java.util.Enumeration;
import java.util.NoSuchElementException;

public final class NoEnumeration<E> implements Enumeration<E> {
    @Override
    public boolean hasMoreElements() {
        return false;
    }

    @Override
    public E nextElement() {
        throw new NoSuchElementException();
    }
}
