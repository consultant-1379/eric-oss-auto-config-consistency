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

package com.ericsson.oss.apps.executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.oss.apps.validation.ModelManager;

import jakarta.annotation.PostConstruct;

@Component
public class ConditionsProcessorInitializer {
    @Autowired
    private ModelManager modelManager;

    @PostConstruct
    public void init() {
        ConditionsProcessor.setModelManager(modelManager);
    }
}
