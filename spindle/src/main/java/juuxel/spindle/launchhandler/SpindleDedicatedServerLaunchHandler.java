/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.launchhandler;

import java.util.Optional;

public final class SpindleDedicatedServerLaunchHandler extends SpindleLaunchHandler {
    @Override
    public String name() {
        return "spindleserver";
    }

    @Override
    public Optional<String> side() {
        return Optional.of("server");
    }
}
