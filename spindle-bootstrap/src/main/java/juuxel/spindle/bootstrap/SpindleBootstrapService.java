/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.bootstrap;

import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService;

import java.nio.file.Path;
import java.util.List;

public final class SpindleBootstrapService implements ITransformerDiscoveryService {
    @Override
    public List<NamedPath> candidates(Path gameDirectory) {
        return null; // TODO: Implement
    }
}
