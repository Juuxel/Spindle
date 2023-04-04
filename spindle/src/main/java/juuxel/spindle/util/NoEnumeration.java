/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

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
