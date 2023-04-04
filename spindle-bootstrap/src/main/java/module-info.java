/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

module io.github.juuxel.spindle.bootstrap {
    requires cpw.mods.modlauncher;
    provides cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService
        with juuxel.spindle.bootstrap.SpindleBootstrapService;
}
