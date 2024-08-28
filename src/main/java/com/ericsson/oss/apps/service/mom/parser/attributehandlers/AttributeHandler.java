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

import java.util.List;
import java.util.Map;

import com.ericsson.oss.apps.service.mom.model.AbstractModelAttribute;
import com.ericsson.oss.mediation.modeling.yangtools.parser.model.statements.AbstractStatement;

/**
 * Implement this to extract the different types of yang attributes i.e.leaf, leaf-list, container and list.
 * 
 * @param <T>
 *            an {@link AbstractStatement}(s) representing the attribute
 */
public interface AttributeHandler<T extends AbstractStatement> {
    /**
     * Extracts the model information from the yang attribute required by eacc.
     * 
     * @param statement
     *            a <code>List</code> of {@link AbstractStatement}(s) representing the attribute
     * @return a <code>Map</code> of {@link AbstractModelAttribute}(s) keyed by the attribute name.
     */
    Map<String, ? extends AbstractModelAttribute> handle(List<T> statement); //NOSONAR Rule:Remove usage of generic wildcard type
}
