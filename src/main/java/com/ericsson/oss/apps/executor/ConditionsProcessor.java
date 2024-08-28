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

import static com.ericsson.oss.apps.util.Constants.FDN;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.util.ConditionsUtils;
import com.ericsson.oss.apps.validation.ModelManager;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public final class ConditionsProcessor {
    private static ModelManager modelManager;

    public static void setModelManager(final ModelManager modelManager) {
        ConditionsProcessor.modelManager = modelManager;
    }

    public static boolean evaluate(final String nonNullCondition, final String moType, final EaccManagedObject managedObject) {
        boolean evaluationResult = false;
        try {
            final String spelExpression = ConditionsUtils.convertToSpel(nonNullCondition, moType);
            final Map<String, Object> contextData = prepareInput(nonNullCondition, moType, managedObject);
            evaluationResult = evaluateSpelExpr(spelExpression, contextData);
        } catch (final Exception e) {
            final String errorMessage = String.format("Condition evaluation failed for '%s'", nonNullCondition);
            log.error(errorMessage, e);
        }
        return evaluationResult;
    }

    private static boolean evaluateSpelExpr(final String spelExpression, final Map<String, Object> contextData) {
        final ExpressionParser parser = new SpelExpressionParser();
        final EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().withInstanceMethods().build();
        contextData.forEach(context::setVariable);

        final String logMessage = "Expression to be evaluated : '{}'";
        final String encoding = "UTF-8";
        try {
            log.debug(logMessage, URLEncoder.encode(spelExpression, encoding));
        } catch (final UnsupportedEncodingException e) { //NOSONAR no need to log exception explicitly
            log.warn("URL Encoding not recognised :{}", "UTF-8");
            log.debug(logMessage, spelExpression);
        }

        final boolean result = (Boolean) parser.parseExpression(spelExpression).getValue(context);
        log.debug("Expression evaluation result '{}'", result);
        return result;
    }

    private static Map<String, Object> prepareInput(final String condition, final String moType, final EaccManagedObject managedObject) {
        final Set<String> attributes = ConditionsUtils.extractAttributeNamesFromConditions(condition, moType);
        final Map<String, Object> contextData = new HashMap<>();
        for (final String attribute : attributes) {
            final String dataType = ConditionsProcessor.modelManager.getDataType(moType, attribute, managedObject.getCmHandle());
            handleDataType(attribute, dataType, (String) managedObject.getAttributes().get(attribute), contextData);
        }
        if (condition.contains(FDN)) {
            contextData.put(FDN.trim(), managedObject.getFdn());
        }
        return contextData;
    }

    private static void handleDataType(final String attribute, final String dataType, final String attributeValue,
            final Map<String, Object> contextData) {
        final Optional<Object> value = switch (dataType) {
            case "boolean" -> Optional.of(Boolean.parseBoolean(attributeValue.toLowerCase(Locale.ROOT)));
            case "int8" -> Optional.of(Byte.parseByte(attributeValue));
            case "int16", "uint8" -> Optional.of(Short.parseShort(attributeValue));
            case "int32", "uint16" -> Optional.of(Integer.parseInt(attributeValue));
            case "int64", "uint32", "uint64" -> Optional.of(Long.parseLong(attributeValue));
            case "decimal64" -> Optional.of(Double.parseDouble(attributeValue));
            case "string", "enumeration" -> Optional.of(attributeValue);
            default -> {
                log.error("Unsupported data type : '{}'", dataType);
                yield Optional.empty();
            }
        };
        value.ifPresent(o -> contextData.put(attribute, o));
    }
}
