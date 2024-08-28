/*******************************************************************************
 * COPYRIGHT Ericsson 2023 - 2024
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

package com.ericsson.oss.apps.repository;

import static com.ericsson.oss.apps.util.Constants.FILTER_NAME_FDN;
import static com.ericsson.oss.apps.util.StringUtil.isNullOrBlank;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FilterCriteria {
    private static final long serialVersionUID = 2338626292552177485L;
    private static final String SEPARATOR = ":";
    private static final String EMPTY_STRING = "";

    private String key;
    private QueryOperator operation;
    private Object value;
    private List<String> values; //Used for in type operations

    public static Optional<FilterCriteria> build(final String filter) {
        final List<String> tokens = checkFilterFormat(filter);
        if (tokens.isEmpty()) {
            return Optional.empty();
        }

        final String filterKey = tokens.get(0);
        final String filterValue = tokens.get(1);
        final char startCharForFilterValue = filterValue.charAt(0);
        final char endCharForFilterValue = filterValue.charAt(filterValue.length() - 1);

        if (!FILTER_NAME_FDN.equals(filterKey) && startCharForFilterValue == '(' && endCharForFilterValue == ')') {
            final List<String> list = Arrays.stream(filterValue.replace("(", "").replace(")", "")
                    .split(",")).toList();
            return Optional.of(new FilterCriteria(filterKey, QueryOperator.IN, null, list));
        } else if (startCharForFilterValue != '%' && endCharForFilterValue != '%') { //NOPMD
            // no % on either side of string
            if (startCharForFilterValue == '!') {
                final String trimExclamationFromFilterValue = filterValue.substring(1)
                        .toLowerCase(Locale.ROOT).replace(' ', '_');
                return Optional.of(new FilterCriteria(filterKey, QueryOperator.NOT_EQUALS, trimExclamationFromFilterValue, null));
            } else {
                return Optional.of(new FilterCriteria(filterKey, QueryOperator.EQUALS, filterValue, null));
            }
        } else if (FILTER_NAME_FDN.equals(filterKey) && startCharForFilterValue != '(') {
            return Optional.of(createLikeFilterCriteria(filterKey, filterValue, startCharForFilterValue,
                    endCharForFilterValue));
        }
        return Optional.empty();
    }

    private static FilterCriteria createLikeFilterCriteria(
            final String filterKey, final String filterValue, final char startCharForFilterValue, final char endCharForFilterValue) {
        if (startCharForFilterValue == '%' && endCharForFilterValue == '%') {
            // % on both sides of string
            final String trimPercentageFromFilterValue = filterValue.substring(1, filterValue.length() - 1).toLowerCase(Locale.ROOT);
            return new FilterCriteria(filterKey, QueryOperator.LIKE, trimPercentageFromFilterValue, null);
        } else if (endCharForFilterValue == '%') {
            // % on right side of string
            final String trimPercentageFromFilterValue = filterValue.substring(0, filterValue.length() - 1).toLowerCase(Locale.ROOT);
            return new FilterCriteria(filterKey, QueryOperator.RIGHT_LIKE, trimPercentageFromFilterValue, null);
        } else {
            // % on left side of string
            final String trimPercentageFromFilterValue = filterValue.substring(1).toLowerCase(Locale.ROOT);
            return new FilterCriteria(filterKey, QueryOperator.LEFT_LIKE, trimPercentageFromFilterValue, null);
        }
    }

    private static List<String> checkFilterFormat(final String filter) {

        if (isNullOrBlank(filter) || filter.indexOf(SEPARATOR) == -1) {
            return Collections.emptyList();
        }

        final int splitIndex = filter.indexOf(SEPARATOR);
        final String keyToken = filter.substring(0, splitIndex);
        final String valueToken = filter.substring(splitIndex + 1);
        final List<String> tokens = List.of(keyToken, valueToken);

        if (tokens.size() != 2 || isNullOrBlank(tokens.get(0)) || isNullOrBlank(tokens.get(1))) {
            return Collections.emptyList();
        }

        return tokens;
    }
}
