/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

package com.ericsson.oss.apps.service.mom.parser;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import com.ericsson.oss.apps.service.ncmp.model.CMHandleModuleRevision;

/**
 * Tests for {@link ModelParser} for exception senario when {@link ModelParserException} should be thrown.
 */
class ModelParserExceptionTest {

    @Test
    void modelsCheckSumTest() {
        final ModelParser modelParser = new ModelParser();
        final List<CMHandleModuleRevision> cmHandleModuleRevisions = new ArrayList<>();
        cmHandleModuleRevisions.add(new CMHandleModuleRevision("whatever", "xxxxxxxxxxxx", "xxxxxxxxxxxx"));

        assertThatThrownBy(() -> modelParser.generateModel(cmHandleModuleRevisions,
                ResourceUtils.getFile("classpath:" + "model_parser_test_files/modules")))
                        .isInstanceOf(ModelParserException.class)
                        .hasMessage("Failed to generate models");
    }
}