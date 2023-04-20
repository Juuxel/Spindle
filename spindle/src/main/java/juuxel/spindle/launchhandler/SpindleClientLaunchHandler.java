/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.launchhandler;

import net.fabricmc.api.EnvType;

public final class SpindleClientLaunchHandler extends SpindleLaunchHandler {
    @Override
    public String name() {
        return "spindleclient";
    }

    @Override
    public EnvType side() {
        return EnvType.CLIENT;
    }
}
