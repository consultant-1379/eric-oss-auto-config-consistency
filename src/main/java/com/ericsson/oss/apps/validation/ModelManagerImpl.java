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

package com.ericsson.oss.apps.validation;

import static com.ericsson.oss.apps.util.Constants.MOC_HYPHEN;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.service.mom.model.LeafModelAttribute;
import com.ericsson.oss.apps.service.mom.model.Model;
import com.ericsson.oss.apps.service.mom.model.ModelObject;
import com.ericsson.oss.apps.service.mom.model.Models;
import com.ericsson.oss.apps.service.ncmp.model.CMHandleModuleRevision;
import com.ericsson.oss.apps.validation.validators.LeafAttributeValueValidator;

import lombok.Getter;
import lombok.Setter;

/**
 * Implementation of the Model Manager.
 */
@Service
public class ModelManagerImpl implements ModelManager {
    private static final String MOC_HYPHEN_MOTYPE = MOC_HYPHEN + "%s";
    private static final String LEAF = "leaf";
    @Getter
    @Setter
    private Models models;

    @Getter
    @Setter
    private List<CMHandleModuleRevision> cmHandleModuleRevisionList;

    @Getter
    @Setter
    private boolean modelValidationReady;

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean isValidMo(final String moType) {
        return models.getModelsMapKeyedByModuleNameAndRevision().values().stream()
                .map(Model::getModelObjectsMapKeyedByName)
                .anyMatch(mosByNameMap -> mosByNameMap.containsKey(MOC_HYPHEN + moType));
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean isValidMo(final String moType, final String cmHandle) {
        return cmHandleModuleRevisionList.stream()
                .filter(cmHandleModuleRevision -> cmHandle.equals(cmHandleModuleRevision.getCMHandle()))
                .map(CMHandleModuleRevision::toNameAndRevision)
                .map(models.getModelsMapKeyedByModuleNameAndRevision()::get)
                .map(Model::getModelObjectsMapKeyedByName)
                .anyMatch(mosByNameMap -> mosByNameMap.containsKey(MOC_HYPHEN + moType));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidAttribute(final String moType, final String attributeName) {
        return models.getModelsMapKeyedByModuleNameAndRevision().values().stream()
                .map(Model::getModelObjectsMapKeyedByName)
                .filter(mosByNameMap -> mosByNameMap.containsKey(MOC_HYPHEN + moType))
                .map(mosByNameMap -> mosByNameMap.get(MOC_HYPHEN + moType))
                .map(ModelObject::getAttributesMapKeyedByAttributeName)
                .anyMatch(attributesByNameMap -> attributesByNameMap.containsKey(attributeName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidAttribute(final String moType, final String attributeName, final String cmHandle) {
        return cmHandleModuleRevisionList.stream()
                .filter(cmHandleModuleRevision -> cmHandle.equals(cmHandleModuleRevision.getCMHandle()))
                .map(CMHandleModuleRevision::toNameAndRevision)
                .filter(models.getModelsMapKeyedByModuleNameAndRevision()::containsKey)
                .map(models.getModelsMapKeyedByModuleNameAndRevision()::get)
                .map(Model::getModelObjectsMapKeyedByName)
                .filter(mosByNameMap -> mosByNameMap.containsKey(MOC_HYPHEN + moType))
                .map(mosByNameMap -> mosByNameMap.get(MOC_HYPHEN + moType))
                .map(ModelObject::getAttributesMapKeyedByAttributeName)
                .anyMatch(attributesByNameMap -> attributesByNameMap.containsKey(attributeName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidValue(final String moType, final String attributeName, final Object attributeValue) {
        return models.getModelsMapKeyedByModuleNameAndRevision().values().stream()
                .map(Model::getModelObjectsMapKeyedByName)
                .filter(mosByNameMap -> mosByNameMap.containsKey(MOC_HYPHEN_MOTYPE.formatted(moType)))
                .map(mosByNameMap -> mosByNameMap.get(MOC_HYPHEN_MOTYPE.formatted(moType)))
                .map(ModelObject::getAttributesMapKeyedByAttributeName)
                .filter(attributesByName -> attributesByName.containsKey(attributeName))
                .map(attributesByName -> attributesByName.get(attributeName))
                .filter(attribute -> LEAF.equals(attribute.getType()))
                .anyMatch(attribute -> LeafAttributeValueValidator.isValidValue(attribute, attributeValue));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidValue(final String moType, final String attributeName, final Object attributeValue, final String cmHandle) {
        return cmHandleModuleRevisionList.stream()
                .filter(cmHandleModuleRevision -> cmHandle.equals(cmHandleModuleRevision.getCMHandle()))
                .map(CMHandleModuleRevision::toNameAndRevision)
                .map(models.getModelsMapKeyedByModuleNameAndRevision()::get)
                .map(Model::getModelObjectsMapKeyedByName)
                .filter(mosByNameMap -> mosByNameMap.containsKey(MOC_HYPHEN + moType))
                .map(mosByNameMap -> mosByNameMap.get(MOC_HYPHEN + moType))
                .map(ModelObject::getAttributesMapKeyedByAttributeName)
                .filter(attributesByNameMap -> attributesByNameMap.containsKey(attributeName))
                .map(attributesByNameMap -> attributesByNameMap.get(attributeName))
                .filter(modelAttribute -> LEAF.equals(modelAttribute.getType()))
                .anyMatch(modelAttribute -> LeafAttributeValueValidator.isValidValue(modelAttribute, attributeValue));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDataType(final String moType, final String attributeName, final String cmHandle) {
        final LeafModelAttribute leafAttr = (LeafModelAttribute) models.getModelsMapKeyedByModuleNameAndRevision().values().stream()
                .map(Model::getModelObjectsMapKeyedByName)
                .filter(mosByNameMap -> mosByNameMap.containsKey(MOC_HYPHEN_MOTYPE.formatted(moType)))
                .map(mosByNameMap -> mosByNameMap.get(MOC_HYPHEN_MOTYPE.formatted(moType)))
                .map(ModelObject::getAttributesMapKeyedByAttributeName)
                .filter(attributesByName -> attributesByName.containsKey(attributeName))
                .map(attributesByName -> attributesByName.get(attributeName))
                .filter(attribute -> LEAF.equals(attribute.getType()))
                .findFirst().orElseThrow();
        return leafAttr.getDataType();
    }
}
