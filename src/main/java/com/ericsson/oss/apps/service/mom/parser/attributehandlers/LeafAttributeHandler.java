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

package com.ericsson.oss.apps.service.mom.parser.attributehandlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ericsson.oss.apps.service.mom.model.LeafModelAttribute;
import com.ericsson.oss.mediation.modeling.yangtools.parser.model.statements.ExtensionStatement;
import com.ericsson.oss.mediation.modeling.yangtools.parser.model.statements.yang.YLeaf;

/**
 * {@inheritDoc}.
 */
public class LeafAttributeHandler implements AttributeHandler<YLeaf> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, LeafModelAttribute> handle(final List<YLeaf> leafs) {
        final Map<String, LeafModelAttribute> modelAttributes = new HashMap<>();

        for (final YLeaf leaf : leafs) {
            final LeafModelAttribute attribute = new LeafModelAttribute();
            attribute.setName(leaf.getLeafName());
            attribute.setType("leaf");
            attribute.setDataType(leaf.getType().getDataType());

            handleConstraints(leaf, attribute);

            handleExtensions(leaf, attribute);
            modelAttributes.put(attribute.getName(), attribute);
        }
        return modelAttributes;
    }

    private void handleExtensions(final YLeaf leaf, final LeafModelAttribute attribute) {
        final List<ExtensionStatement> extensions = leaf.getExtensionChildStatements();
        for (final ExtensionStatement extension : extensions) {
            if (extension.getStatementName().equals("takes-effect")) {
                attribute.setTakesEffect(extension.getValue());
            } else if (extension.getStatementName().equals("precondition")) {
                attribute.setPreCondition(extension.getValue());
            }

        }
    }

    private void handleConstraints(final YLeaf leaf, final LeafModelAttribute attribute) {
        if (leaf.getType().getRange() != null) { //numbers i.e. int32 etc..
            attribute.setValueConstraint(leaf.getType().getRange().getRangeValues());
        } else if (leaf.getType().getDataType().equals("enumeration")) { //enums
            attribute.setValueConstraint(leaf.getType().getEnums().toString());
        } else if (leaf.getType().getDataType().equals("string") && leaf.getType().getLength() != null) {
            attribute.setValueConstraint(leaf.getType().getLength().toString());
        }
    }
}
