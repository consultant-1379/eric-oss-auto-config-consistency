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

package com.ericsson.oss.apps.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.util.ResourceUtils;

import com.ericsson.oss.apps.service.mom.model.Models;
import com.ericsson.oss.apps.service.mom.parser.ModelParser;
import com.ericsson.oss.apps.service.mom.parser.ModelParserException;
import com.ericsson.oss.apps.service.ncmp.model.CMHandleModuleRevision;

/**
 * Unit tests for {@link ModelManagerImpl} class.
 */
@ExtendWith(SoftAssertionsExtension.class)
class ModelManagerImplTest {

    private static final ModelManagerImpl objUnderTest = new ModelManagerImpl();
    private static final List<CMHandleModuleRevision> cmHandleModuleRevisions = new ArrayList<>();
    private static final ModelParser modelParser = new ModelParser();

    @BeforeAll
    static void setUp() throws FileNotFoundException, ModelParserException {
        cmHandleModuleRevisions.add(new CMHandleModuleRevision("cmHandle1", "ericsson-enm-Lrat", "2067-10-22"));
        cmHandleModuleRevisions.add(new CMHandleModuleRevision("cmHandle2", "ericsson-enm-Lrat", "2351-11-28"));
        cmHandleModuleRevisions.add(new CMHandleModuleRevision("cmHandle2", "ericsson-enm-GNBCUCP", "2351-11-28"));
        cmHandleModuleRevisions.add(new CMHandleModuleRevision("cmHandle2", "ericsson-enm-GNBDU", "2351-11-28"));
        cmHandleModuleRevisions.add(new CMHandleModuleRevision("cmHandle2", "ericsson-enm-GNBCUUP", "2351-11-28"));
        cmHandleModuleRevisions.add(new CMHandleModuleRevision("cmHandle3", "ericsson-enm-GNBCUUP", "2966-10-03"));
        final Models models = modelParser.generateModel(cmHandleModuleRevisions,
                ResourceUtils.getFile("classpath:" + "model_parser_test_files/modules"));
        objUnderTest.setModels(models);
        objUnderTest.setCmHandleModuleRevisionList(cmHandleModuleRevisions);
    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/model_parser_test_files/model_object_exists_4G.csv",
            "/model_parser_test_files/model_object_exists_5G.csv" }, delimiter = ':', numLinesToSkip = 1)
    void isValidMoTest(final String moType, final String exists) {
        assertThat(objUnderTest.isValidMo(moType)).isEqualTo(Boolean.parseBoolean(exists));
    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/model_parser_test_files/model_object_attribute_exists_4G.csv",
            "/model_parser_test_files/model_object_attribute_exists_5G.csv" }, delimiter = ':', numLinesToSkip = 1)
    void isValidAttributeTest(final String moType, final String attributeName, final String exists) {
        assertThat(objUnderTest.isValidAttribute(moType,
                attributeName)).isEqualTo(Boolean.parseBoolean(exists));
    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/model_manager_test_files/model_object_exists_node_level_4G.csv",
            "/model_manager_test_files/model_object_exists_node_level_5G.csv" }, delimiter = ':', numLinesToSkip = 1)
    void isValidMoAtNodeLevelTest(final String moType, final String cmHandle, final String exists) {
        assertThat(objUnderTest.isValidMo(moType, cmHandle)).isEqualTo(Boolean.parseBoolean(exists));
    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/model_manager_test_files/model_object_attribute_exists_node_level_4G.csv",
            "/model_manager_test_files/model_object_attribute_exists_node_level_5G.csv" }, delimiter = ':', numLinesToSkip = 1)
    void isValidAttrAtNodeLevelTest(final String moType, final String attributeName, final String cmHandle, final String exists) {
        assertThat(objUnderTest.isValidAttribute(moType, attributeName, cmHandle)).isEqualTo(Boolean.parseBoolean(exists));

    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/model_manager_test_files/model_object_attribute_value_valid_4G.csv",
            "/model_manager_test_files/model_object_attribute_value_valid_5G.csv" }, delimiter = ':', numLinesToSkip = 1)
    void isValidValueTest(final String moType, final String attributeName, final String attributeValue, final String valid) {
        assertThat(objUnderTest.isValidValue(moType, attributeName, attributeValue)).isEqualTo(Boolean.parseBoolean(valid));
    }

    @ParameterizedTest
    @CsvFileSource(resources = {
            "/model_manager_test_files/model_object_attribute_value_valid_node_level_4G.csv",
            "/model_manager_test_files/model_object_attribute_value_valid_node_level_5G.csv" }, delimiter = ':', numLinesToSkip = 1)
    void isValidValueAtNodeLevelTest(final String moType, final String attributeName, final String attributeValue, final String cmHandle,
            final String valid) {
        assertThat(objUnderTest.isValidValue(moType, attributeName, attributeValue, cmHandle)).isEqualTo(Boolean.parseBoolean(valid));
    }

    @ParameterizedTest
    @CsvFileSource(resources = {
            "/model_manager_test_files/model_object_attribute_get_datatype_node_level_4G.csv"}, delimiter = ':', numLinesToSkip = 1)
    void getDataTypeAtNodeLevelTest(final String moType, final String attributeName, final String cmHandle,
                                    final String dataType) {
        assertThat(objUnderTest.getDataType(moType, attributeName, cmHandle)).isEqualTo(dataType);
    }
}
