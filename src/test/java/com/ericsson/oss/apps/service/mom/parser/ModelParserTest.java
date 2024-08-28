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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.springframework.util.ResourceUtils;

import com.ericsson.oss.apps.service.mom.model.AbstractModelAttribute;
import com.ericsson.oss.apps.service.mom.model.LeafModelAttribute;
import com.ericsson.oss.apps.service.mom.model.Model;
import com.ericsson.oss.apps.service.mom.model.ModelObject;
import com.ericsson.oss.apps.service.mom.model.Models;
import com.ericsson.oss.apps.service.ncmp.model.CMHandleModuleRevision;

/**
 * ParameterizedTest Tests for {@link ModelParser} class <br>
 * <br>
 * <b>Note:</b> In csv files empty is used to represent null @see
 * https://stackoverflow.com/questions/65207427/junit-5-how-to-pass-in-multiple-null-values-for-csvsource
 */
class ModelParserTest {
    static List<CMHandleModuleRevision> uniqueCMHandleModuleRevisions = new ArrayList<>();
    static Models models;
    static final ModelParser modelParser = new ModelParser();

    @BeforeAll
    static void before() throws Exception {
        uniqueCMHandleModuleRevisions.add(new CMHandleModuleRevision("whatever", "ericsson-enm-Lrat", "2351-11-28"));
        uniqueCMHandleModuleRevisions.add(new CMHandleModuleRevision("whatever", "ericsson-enm-GNBCUCP", "2351-11-28"));
        uniqueCMHandleModuleRevisions.add(new CMHandleModuleRevision("whatever", "ericsson-enm-GNBDU", "2351-11-28"));
        uniqueCMHandleModuleRevisions.add(new CMHandleModuleRevision("whatever", "ericsson-enm-GNBCUUP", "2351-11-28"));
        models = modelParser.generateModel(uniqueCMHandleModuleRevisions,
                ResourceUtils.getFile("classpath:" + "model_parser_test_files/modules"));
    }

    @AfterAll
    static void after() {
        models = null;
    }

    @Test
    void modelsCheckSumTest() {

        assertThat(models.getModelsMapKeyedByModuleNameAndRevision()).hasSize(4);
    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/model_parser_test_files/model_objects_checksum.csv" }, delimiter = ':', numLinesToSkip = 1)
    void modelObjectsCheckSumTest(final String moduleName, final String moduleRevision, final String modelObjectsCount) {
        final Model model = models.getModelsMapKeyedByModuleNameAndRevision().get(moduleName + ":" + moduleRevision);
        assertThat(model.getModelObjectsMapKeyedByName()).hasSize(Integer.parseInt(modelObjectsCount));
    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/model_parser_test_files/model_object_leaf_attributes_checksum.csv" }, delimiter = ':', numLinesToSkip = 1)
    void modelObjectsAttributesCheckSumTest(final String moduleName, final String moduleRevision, final String managedObjectName,
            final String attributesCount) {
        final Model model = models.getModelsMapKeyedByModuleNameAndRevision().get(moduleName + ":" + moduleRevision);
        assertThat(model.getModelObjectsMapKeyedByName().get("moc-" + managedObjectName).getAttributesMapKeyedByAttributeName())
                .hasSize(Integer.parseInt(attributesCount));
    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/model_parser_test_files/model_object_exists_4G.csv",
            "/model_parser_test_files/model_object_exists_5G.csv" }, delimiter = ':', numLinesToSkip = 1)
    void modelObjectExistsTest(final String managedObjectName, final String exists) {
        boolean found = false;
        for (final Map.Entry<String, Model> model : models.getModelsMapKeyedByModuleNameAndRevision().entrySet()) {
            if (model.getValue().getModelObjectsMapKeyedByName().containsKey("moc-" + managedObjectName)) {
                found = true;
                break;
            }
        }
        assertThat(found).isEqualTo(Boolean.parseBoolean(exists));
    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/model_parser_test_files/model_object_attribute_exists_4G.csv",
            "/model_parser_test_files/model_object_attribute_exists_5G.csv"

    }, delimiter = ':', numLinesToSkip = 1)
    void modelObjectAttributeExistsTest(final String managedObjectName, final String attributeName, final String exists) {
        boolean found = false;
        ModelObject modelObject;
        for (final Map.Entry<String, Model> model : models.getModelsMapKeyedByModuleNameAndRevision().entrySet()) {
            modelObject = model.getValue().getModelObjectsMapKeyedByName().get("moc-" + managedObjectName);
            if (modelObject != null && modelObject.getAttributesMapKeyedByAttributeName()
                    .containsKey(attributeName)) {
                found = true;
                break;
            }
        }
        assertThat(found).isEqualTo(Boolean.parseBoolean(exists));
    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/model_parser_test_files/model_object_attribute_contents_correct_4G.csv",
            "/model_parser_test_files/model_object_attribute_contents_correct_5G.csv"
    }, delimiter = ':', numLinesToSkip = 1)
    void modelObjectAttributeContentCorrectTest(final String managedObjectName,
            final String attributeName, final String type,
            final String dataType,
            final String valueConstraint, final String takesEffect, final String preCondition) {
        boolean found = false;
        ModelObject modelObject;

        for (final Map.Entry<String, Model> model : models.getModelsMapKeyedByModuleNameAndRevision().entrySet()) {
            modelObject = model.getValue().getModelObjectsMapKeyedByName().get("moc-" + managedObjectName);
            if (modelObject != null) {
                found = checkAttributeContent(managedObjectName, attributeName, type, dataType, valueConstraint, takesEffect, preCondition,
                        model.getValue());
                if (found) {
                    break;
                }
            }
        }
        if (!found) {
            fail("Failed for entry %s:%s:%s:%s:%s:%s:%s", managedObjectName, attributeName, type, dataType, valueConstraint, takesEffect,
                    preCondition);
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = { "/model_parser_test_files/model_object_attribute_contents_correct_when_in_multiple_models.csv"
    }, delimiter = ':', numLinesToSkip = 1)
    void modelObjectAttributeContentCorrectForSpecificModelTest(final String moduleName, final String moduleRevision,
            final String managedObjectName, final String attributeName, final String type,
            final String dataType,
            final String valueConstraint, final String takesEffect, final String preCondition) {
        boolean found = false;
        final ModelObject modelObject;

        final Model model = models.getModelsMapKeyedByModuleNameAndRevision().get(moduleName + ":" + moduleRevision);
        modelObject = model.getModelObjectsMapKeyedByName().get("moc-" + managedObjectName);
        if (modelObject != null) {
            found = checkAttributeContent(managedObjectName, attributeName, type, dataType, valueConstraint, takesEffect, preCondition, model);

        }

        if (!found) {
            fail("Failed for entry %s:%s:%s:%s:%s:%s:%s:%s:%s", moduleName, moduleRevision, managedObjectName, attributeName, type, dataType,
                    valueConstraint, takesEffect,
                    preCondition);
        }
    }

    private boolean checkAttributeContent(final String managedObjectName, final String attributeName, final String type, final String dataType,
            final String valueConstraint,
            final String takesEffect, final String preCondition, final Model model) {
        boolean found = false;
        final AbstractModelAttribute attribute;
        attribute = model.getModelObjectsMapKeyedByName().get("moc-" + managedObjectName)
                .getAttributesMapKeyedByAttributeName()
                .get(attributeName);
        if (attribute != null) {
            final LeafModelAttribute leafModelAttribute = (LeafModelAttribute) attribute;
            assertThat(leafModelAttribute.getName()).isEqualTo(attributeName);
            assertThat(leafModelAttribute.getType()).isEqualTo(type);
            assertThat(leafModelAttribute.getDataType()).isEqualTo(dataType);
            assertThat(leafModelAttribute.getValueConstraint()).isEqualTo(valueConstraint);
            assertThat(leafModelAttribute.getTakesEffect()).isEqualTo(takesEffect);
            assertThat(leafModelAttribute.getPreCondition()).isEqualTo(preCondition);
            found = true;
        }
        return found;
    }
}