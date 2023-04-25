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
            joiner.add(escape(part));
            start = dashIndex + 1;
        }

        // Add the remaining part of the id.
        String part = id.substring(start);
        joiner.add(escape(part));

        return joiner.toString();
    }

    // Identifier character set = [a-z0-9-_]
    private static String escape(String identifier) {
        if (identifier.isEmpty()) {
            return "$";
        } else if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            return "$" + identifier;
        } else {
            return identifier;
        }
    }
}
