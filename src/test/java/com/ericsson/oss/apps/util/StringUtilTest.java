/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.apps.util;

import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StringUtil} class.
 */
class StringUtilTest {

    private static final String STRING = "string";

    @Test
    void whenIsNullOrBlank_andNull_thenReturnTrue() {
        assertThat(StringUtil.isNullOrBlank(null)).isTrue();
    }

    @Test
    void whenIsNullOrBlank_andEmpty_thenReturnTrue() {
        assertThat(StringUtil.isNullOrBlank(EMPTY_STRING)).isTrue();
    }

    @Test
    void whenIsNullOrBlank_andWhitespaceOnly_thenReturnTrue() {
        assertThat(StringUtil.isNullOrBlank("  \n ")).isTrue();
    }

    @Test
    void whenIsNullOrBlank_andContainsNonWhitespace_thenReturnFalse() {
        assertThat(StringUtil.isNullOrBlank("a")).isFalse();
    }

    @Test
    void whenContainsExactlyOnce_andEmptyStringAndSubstring_ReturnTrue() {
        assertThat(StringUtil.containsExactlyOnce(EMPTY_STRING, EMPTY_STRING)).isFalse();
    }

    @Test
    void whenContainsExactlyOnce_andNullString_ReturnFalse() {
        assertThat(StringUtil.containsExactlyOnce(null, STRING)).isFalse();
    }

    @Test
    void whenContainsExactlyOnce_andNullSubstring_ReturnFalse() {
        assertThat(StringUtil.containsExactlyOnce(STRING, null)).isFalse();
    }

    @Test
    void whenContainsExactlyOnce_andEmptySubstring_ReturnTrue() {
        assertThat(StringUtil.containsExactlyOnce(STRING, EMPTY_STRING)).isFalse();
    }

    @Test
    void whenContainsExactlyOnce_andSingleCharacterStringAndSubstringEqual_ReturnTrue() {
        assertThat(StringUtil.containsExactlyOnce("a", "a")).isTrue();
    }

    @Test
    void whenContainsExactlyOnce_andMultipleCharacterStringAndSubstringEqual_ThenReturnTrue() {
        assertThat(StringUtil.containsExactlyOnce("aa", "aa")).isTrue();
    }

    @Test
    void whenContainsExactlyOnce_andSubstringOverlaps_ReturnFalse() {
        assertThat(StringUtil.containsExactlyOnce("aaa", "aa")).isFalse();
    }

    @Test
    void whenContainsExactlyOnce_andMultiCharSubstringNotFound_ReturnFalse() {
        assertThat(StringUtil.containsExactlyOnce(STRING, "char")).isFalse();
    }

    @Test
    void whenContainsExactlyOnce_andSubstringNotFound_ReturnFalse() {
        assertThat(StringUtil.containsExactlyOnce(STRING, "d")).isFalse();
    }

    @Test
    void whenContainsExactlyOnce_andSingleCharSubstringRepeated_ReturnFalse() {
        assertThat(StringUtil.containsExactlyOnce("abcabc", "ab")).isFalse();
    }

    @Test
    void whenContainsExactlyOnce_andMultiCharSubstringRepeated_ReturnFalse() {
        assertThat(StringUtil.containsExactlyOnce("abcabc", "bc")).isFalse();
    }

    @Test
    void whenContainsExactlyOnce_andLastSingleCharMatches_ReturnTrue() {
        assertThat(StringUtil.containsExactlyOnce("abcabcd", "d")).isTrue();
    }
}
