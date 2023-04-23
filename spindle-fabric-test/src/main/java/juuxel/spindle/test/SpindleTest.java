/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.test;

import net.fabricmc.api.ModInitializer;

public class SpindleTest implements ModInitializer {
    @Override
    public void onInitialize() {
        if (!getClass().getModule().isNamed()) {
            throw new AssertionError("Unnamed module for mod");
        }
    }
}
