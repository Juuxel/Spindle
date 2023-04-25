/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.util;

import java.util.StringJoiner;

public final class ModuleNames {
    /**
     * Converts a Fabric mod ID into a valid Java module name.
     *
     * @param id the mod ID
     * @return the module name
     */
    public static String fromModId(String id) {
        StringJoiner joiner = new StringJoiner(".");
        int start = 0;
        int dashIndex;

        while (start < id.length() && (dashIndex = id.indexOf('-', start)) >= 0) {
            String part = id.substring(start, dashIndex);

            if (part.isEmpty()) {
                joiner.add("$");
            } else if (!Character.isJavaIdentifierStart(part.charAt(0))) {
                joiner.add("$" + part);
            } else {
                joiner.add(part);
            }

            start = dashIndex + 1;
        }

        if (start < id.length()) {
            joiner.add(id.substring(start));
        }

        return joiner.toString();
    }
}
