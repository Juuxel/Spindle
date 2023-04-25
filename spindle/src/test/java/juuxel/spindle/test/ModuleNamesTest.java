/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package juuxel.spindle.test;

import juuxel.spindle.util.ModuleNames;
import org.junit.jupiter.api.Test;

import javax.lang.model.SourceVersion;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleNamesTest {
    private static final String SIMPLE_DASH = "tiny-potato";
    private static final String SIMPLE_UNDERSCORE = "tiny_potato";
    private static final String NUMBER_IN_ID = "tiny-carrot-1";
    private static final String NUMBER_IN_ID_MIDDLE = "tiny-carrot-1-test";
    private static final String NUMBER_IN_ID_UNDERSCORE = "tiny-carrot-_1";
    private static final String ENDS_IN_DASH = "tiny-";
    private static final String ENDS_IN_DASHES = "tiny--";
    private static final String CONSECUTIVE_DASHES = "tiny--carrot---or----potato";
    private static final String MIXED_DASHES_AND_UNDERSCORES = "tiny-_-carrot-_-_-_or-_-_-_-__potato";
    private static final List<String> ALL_IDS = List.of(
        SIMPLE_DASH,
        SIMPLE_UNDERSCORE,
        NUMBER_IN_ID,
        NUMBER_IN_ID_MIDDLE,
        NUMBER_IN_ID_UNDERSCORE,
        ENDS_IN_DASH,
        ENDS_IN_DASHES,
        CONSECUTIVE_DASHES,
        MIXED_DASHES_AND_UNDERSCORES
    );

    @Test
    void nameCollisions() {
        assertThat(ModuleNames.fromModId(SIMPLE_DASH))
            .isNotEqualTo(ModuleNames.fromModId(SIMPLE_UNDERSCORE));
        assertThat(ModuleNames.fromModId(NUMBER_IN_ID))
            .isNotEqualTo(ModuleNames.fromModId(NUMBER_IN_ID_UNDERSCORE));
        assertThat(ModuleNames.fromModId(ENDS_IN_DASH))
            .isNotEqualTo(ModuleNames.fromModId(ENDS_IN_DASHES));
    }

    @Test
    void validIdentifier() {
        assertThat(ALL_IDS)
            .map(ModuleNames::fromModId)
            .allSatisfy(name -> {
                var parts = name.split("\\.");
                assertThat(parts)
                    .allMatch(SourceVersion::isIdentifier, "is valid java identifier");
            });
    }

    @Test
    void specificIds() {
        assertThat(ModuleNames.fromModId(SIMPLE_DASH))
            .isEqualTo("tiny.potato");
        assertThat(ModuleNames.fromModId(SIMPLE_UNDERSCORE))
            .isEqualTo("tiny_potato");
        assertThat(ModuleNames.fromModId(NUMBER_IN_ID))
            .isEqualTo("tiny.carrot.$1");
        assertThat(ModuleNames.fromModId(NUMBER_IN_ID_MIDDLE))
            .isEqualTo("tiny.carrot.$1.test");
        assertThat(ModuleNames.fromModId(NUMBER_IN_ID_UNDERSCORE))
            .isEqualTo("tiny.carrot._1");
        assertThat(ModuleNames.fromModId(ENDS_IN_DASH))
            .isEqualTo("tiny.$");
        assertThat(ModuleNames.fromModId(ENDS_IN_DASHES))
            .isEqualTo("tiny.$.$");
        assertThat(ModuleNames.fromModId(CONSECUTIVE_DASHES))
            .isEqualTo("tiny.$.carrot.$.$.or.$.$.$.potato");
        assertThat(ModuleNames.fromModId(MIXED_DASHES_AND_UNDERSCORES))
            .isEqualTo("tiny._.carrot._._._or._._._.__potato");
    }
}
