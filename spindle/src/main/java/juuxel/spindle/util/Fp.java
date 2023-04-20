/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.util;

import java.util.function.BiPredicate;

/**
 * Functional programming utilities.
 */
public final class Fp {
    public static <T, U> BiPredicate<T, U> any(Iterable<? extends BiPredicate<? super T, ? super U>> predicates) {
        return (t, u) -> {
            for (BiPredicate<? super T, ? super U> predicate : predicates) {
                if (predicate.test(t, u)) return true;
            }

            return false;
        };
    }
}
