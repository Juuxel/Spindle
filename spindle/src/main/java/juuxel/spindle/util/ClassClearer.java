/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.util;

public interface ClassClearer<T> {
    void clear(T t);

    static <T> ClassClearer<T> forClass(Class<T> c) {
        return ClassClearerImpl.getClearer(c);
    }
}
