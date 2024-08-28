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

package com.ericsson.oss.apps.service.ncmp;

import java.io.IOException;
import java.util.Map;

import org.springframework.util.ResourceUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.experimental.UtilityClass;

/**
 * Utility used in the tests for {@link NcmpResultsParser} for building the response object from nmcp expected by the parser from a json file.
 */
@UtilityClass
public class NcmpResponseObjectBuilderUtil {
    public static Map<String, Object> buildResponseObjectFromJsonFile(final String fileName) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(ResourceUtils.getFile("classpath:" + fileName), new TypeReference<>() {
        });
    }
}