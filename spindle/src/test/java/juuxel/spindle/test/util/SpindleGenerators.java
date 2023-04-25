/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.test.util;

import org.instancio.Random;
import org.instancio.generator.Generator;

public final class SpindleGenerators {
    private static final String VALID_MOD_ID_STARTS = "abcdefghijklmnopqrstuvwxyz";
    private static final String VALID_MOD_ID_CHARACTERS = VALID_MOD_ID_STARTS + "0123456789-_";
    private static final int MIN_MOD_ID_LENGTH = 2;
    private static final int MAX_MOD_ID_LENGTH = 64;

    /**
     * Generates a random Fabric mod ID.
     */
    public static final Generator<String> RANDOM_MOD_ID = random -> {
        int length = random.intRange(MIN_MOD_ID_LENGTH, MAX_MOD_ID_LENGTH);
        StringBuilder id = new StringBuilder(length);
        id.append(randomCharacter(random, VALID_MOD_ID_STARTS));

        for (int i = 0; i < length - 1; i++) {
            id.append(randomCharacter(random, VALID_MOD_ID_CHARACTERS));
        }

        return id.toString();
    };

    private static char randomCharacter(Random random, String s) {
        return s.charAt(random.intRange(0, s.length() - 1));
    }
}
