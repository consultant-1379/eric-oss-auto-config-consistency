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

package com.ericsson.oss.apps.executor;

import static com.ericsson.oss.apps.util.Constants.COMMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.util.TestDefaults;
import com.ericsson.oss.apps.validation.ModelManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Unit tests for {@link ConsistencyChecker} class.
 */
@Slf4j
@ExtendWith({MockitoExtension.class})
public class ConsistencyCheckerTest {

    private static final String RULES_FILE = "SampleRulesetFile.csv";
    private static final String RULES_WITH_CONDITIONS_FILE = "RulesetWithConditionsConsistencyCheckerTest.csv";
    private static final String RULES_WITH_CONDITIONS_AND_PRIORITY_FILE = "RulesetWithConditionsAndPriorityConsistencyCheckerTest.csv";
    private static final String EACC_MO_FILE_NO_INCONSISTENCIES = "EaccManagedObjectsWithNoInconsistencies.csv";
    private static final String EACC_MO_FILE_WITH_INCONSISTENCIES = "EaccManagedObjectsWithInconsistencies.csv";
    private static final String INT_32 = "int32";
    @Mock
    private ModelManager mockModelManager;
    @BeforeEach
    public void setup() {
        AuditResult.clearIdMap();
    }

    @Test
    public void whenAuditIsPerformed_andNoInconsistenciesExist_thenEmptyListReturnedInResult() throws IOException {
        final Map<String, List<Rule>> ruleSetMap = createRulesetMapForTests(RULES_FILE);
        final List<EaccManagedObject> eaccManagedObjectList = createEaccManagedObjects(EACC_MO_FILE_NO_INCONSISTENCIES);

        final List<AuditResult> auditResultList = new ArrayList<>();
        int totalInconsistencies = 0;
        for (final EaccManagedObject mo : eaccManagedObjectList) {
            final ConsistencyCheckResult result = ConsistencyChecker.check(mo, ruleSetMap.get(mo.getMoType()),
                    "1");
            auditResultList.addAll(result.getAuditResultList());
            totalInconsistencies += result.getInconsistencyCount();
        }
        final List<AuditResult> inconsistentResults = getSpecificAuditResults(auditResultList, AuditStatus.INCONSISTENT);
        assertThat(inconsistentResults).isEmpty();
        assertThat(totalInconsistencies).isZero();
    }

    @Test
    public void whenAuditIsPerformed_andInconsistenciesDoExist_thenAllInconsistenciesAreReturnedInTheResult() throws IOException {
        final Map<String, List<Rule>> ruleSetMap = createRulesetMapForTests(RULES_FILE);
        final List<EaccManagedObject> eaccManagedObjectList = createEaccManagedObjects(EACC_MO_FILE_WITH_INCONSISTENCIES);

        final List<AuditResult> auditResultList = new ArrayList<>();
        int totalInconsistencies = 0;
        for (final EaccManagedObject mo : eaccManagedObjectList) {
            final ConsistencyCheckResult result = ConsistencyChecker.check(mo, ruleSetMap.get(mo.getMoType()),
                    "1");
            auditResultList.addAll(result.getAuditResultList());
            totalInconsistencies += result.getInconsistencyCount();
        }
        final List<AuditResult> inconsistentResults = getSpecificAuditResults(auditResultList, AuditStatus.INCONSISTENT);

        assertThat(inconsistentResults).hasSize(5)
                .extracting(AuditResult::getAttributeName, AuditResult::getCurrentValue, AuditResult::getPreferredValue)
                .containsExactly(
                        tuple(TestDefaults.CELL_CAP_MIN_CELL_SUB_CAP, "null", "1000"),
                        tuple(TestDefaults.CELL_CAP_MIN_MAX_WRI_PROT, "null", "true"),
                        tuple(TestDefaults.CFRA_ENABLE, "false", "true"),
                        tuple(TestDefaults.PRACH_CONFIG_ENABLED, "false", "true"),
                        tuple(TestDefaults.ZZZ_TEMPORARY_74, "0", "-2000000000"));
        assertThat(totalInconsistencies).isEqualTo(5);
    }

    @Test
    public void whenAuditIsPerformed_andInconsistenciesDoExist_thenAllAuditResultsAreUniquePerExecution() throws IOException {
        final Map<String, List<Rule>> ruleSetMap = createRulesetMapForTests(RULES_FILE);
        final List<EaccManagedObject> eaccManagedObjectList = createEaccManagedObjects(EACC_MO_FILE_WITH_INCONSISTENCIES);
        final List<Long> idsUsedByEachExecution = List.of(1L, 2L, 3L, 6L, 7L);
        assertInconsistenciesFoundHaveExecutionAndIdAsKey(ruleSetMap, eaccManagedObjectList, "1", idsUsedByEachExecution);
        assertInconsistenciesFoundHaveExecutionAndIdAsKey(ruleSetMap, eaccManagedObjectList, "2", idsUsedByEachExecution);
    }

    @Test
    public void whenAuditIsPerformed_andConditionsIsNotEmpty_thenAuditResultIsProducedWhenConditionEvaluatesToTrue() throws IOException {
        final Map<String, List<Rule>> ruleSetMap = createRulesetMapForTests(RULES_WITH_CONDITIONS_FILE);
        final List<EaccManagedObject> eaccManagedObjectList = createEaccManagedObjects(EACC_MO_FILE_WITH_INCONSISTENCIES);

        when(mockModelManager.getDataType(anyString(), anyString(), anyString())).
                thenReturn(INT_32);
        ConditionsProcessor.setModelManager(mockModelManager);
        final List<AuditResult> auditResultList = new ArrayList<>();
        for (final EaccManagedObject mo : eaccManagedObjectList) {
            final ConsistencyCheckResult result = ConsistencyChecker.check(mo, ruleSetMap.get(mo.getMoType()),
                    "1");
            auditResultList.addAll(result.getAuditResultList());
        }
        assertThat(auditResultList.size())
                .as("Conditions evaluate to false on one out of three rules ").isEqualTo(2);
    }

    @Test
    public void whenAuditIsPerformed_andMultipleRulesWithPriorityExist_thenAuditResultIsProducedWhenConditionEvaluatesToTrueForHighestPriorityRule () throws IOException {
        final Map<String, List<Rule>> ruleSetMap = createRulesetMapForTests(RULES_WITH_CONDITIONS_AND_PRIORITY_FILE);
        final List<EaccManagedObject> eaccManagedObjectList = createEaccManagedObjects(EACC_MO_FILE_WITH_INCONSISTENCIES);

        when(mockModelManager.getDataType(anyString(), anyString(), anyString())).
                thenReturn(INT_32);
        ConditionsProcessor.setModelManager(mockModelManager);
        final List<AuditResult> auditResultList = new ArrayList<>();
        for (final EaccManagedObject mo : eaccManagedObjectList) {
            final ConsistencyCheckResult result = ConsistencyChecker.check(mo, ruleSetMap.get(mo.getMoType()),
                    "1");
            auditResultList.addAll(result.getAuditResultList());
        }

        assertThat(auditResultList).as("Second highest priority & condition is false.")
                .extracting("ruleId")
                .doesNotContain("1");
        assertThat(auditResultList).as("Higher priority than rule1 but condition is false.")
                .extracting("ruleId")
                .doesNotContain("2");
        assertThat(auditResultList).as("Higher priority than rule 4 but condition is false.")
                .extracting("ruleId")
                .doesNotContain("3");
        assertThat(auditResultList).as("Lower priority than rule 5 and thus is skipped.")
                .extracting("ruleId")
                .doesNotContain("6");
        assertThat(auditResultList).as("Lower priority than rule 3 but condition is true.")
                .extracting("ruleId")
                .contains("4");
        assertThat(auditResultList).as("Higher priority than rule 6 & both Conditions are true.")
                .extracting("ruleId")
                .contains("5");
    }

    private void assertInconsistenciesFoundHaveExecutionAndIdAsKey(final Map<String, List<Rule>> ruleSetMap,
            final List<EaccManagedObject> eaccManagedObjectList,
            final String executionId, final List<Long> expectedUniqueIdsPerExecution) {
        final var auditResultListAndInconsistencies = checkAndReturnResultsAndInconsistencyCount(ruleSetMap, eaccManagedObjectList, executionId);
        final List<AuditResult> auditResultList1 = auditResultListAndInconsistencies.getLeft();
        final int totalInconsistencies1 = auditResultListAndInconsistencies.getRight().get();
        final List<AuditResult> inconsistentResults = getSpecificAuditResults(auditResultList1, AuditStatus.INCONSISTENT);

        assertThat(inconsistentResults).hasSize(5)
                .extracting(AuditResult::getExecutionId, AuditResult::getId)
                .containsExactly(
                        tuple(executionId, expectedUniqueIdsPerExecution.get(0)),
                        tuple(executionId, expectedUniqueIdsPerExecution.get(1)),
                        tuple(executionId, expectedUniqueIdsPerExecution.get(2)),
                        tuple(executionId, expectedUniqueIdsPerExecution.get(3)),
                        tuple(executionId, expectedUniqueIdsPerExecution.get(4)));
        assertThat(totalInconsistencies1).isEqualTo(expectedUniqueIdsPerExecution.size());
    }

    private static Pair<List<AuditResult>, AtomicInteger> checkAndReturnResultsAndInconsistencyCount(final Map<String, List<Rule>> ruleSetMap,
            final List<EaccManagedObject> eaccManagedObjectList, final String executionId) {
        final Pair<List<AuditResult>, AtomicInteger> auditResultListAndInconsistencies = Pair.of(new ArrayList<>(), new AtomicInteger());
        for (final EaccManagedObject mo : eaccManagedObjectList) {
            final ConsistencyCheckResult result = ConsistencyChecker.check(mo, ruleSetMap.get(mo.getMoType()), executionId);
            auditResultListAndInconsistencies.getLeft().addAll(result.getAuditResultList());
            auditResultListAndInconsistencies.getRight().addAndGet(result.getInconsistencyCount());
        }
        return auditResultListAndInconsistencies;
    }

    private List<EaccManagedObject> createEaccManagedObjects(final String fileName) throws IOException {
        final List<EaccManagedObject> eaccManagedObjectList = new ArrayList<>();

        try (final InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
                final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            reader.readLine();
            while ((line = reader.readLine()) != null) {
                final EaccManagedObject eaccManagedObject = new EaccManagedObject();
                final String[] data = line.trim().split(COMMA);
                eaccManagedObject.setMoId(data[0]);
                eaccManagedObject.setMoType(data[1]);
                final Map<String, Object> attributes = Arrays.stream(data[2].split(";"))
                        .map(pair -> pair.split(":"))
                        .filter(keyValue -> keyValue.length == 2)
                        .collect(Collectors.toMap(keyValue -> keyValue[0].trim(),
                                keyValue -> keyValue[1].trim()));
                eaccManagedObject.setAttributes(attributes);
                eaccManagedObject.setCmHandle(data[3]);
                eaccManagedObject.setFdn(data[4]);
                eaccManagedObjectList.add(eaccManagedObject);
            }
        }

        return eaccManagedObjectList;
    }

    private Map<String, List<Rule>> createRulesetMapForTests(final String fileName) {
        final Map<String, List<Rule>> ruleSetMap = new HashMap<>();

        try (final InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
                final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            reader.readLine();
            while ((line = reader.readLine()) != null) {

                final Rule newRule = new Rule();
                final String[] data = line.trim().split(COMMA);
                newRule.setId(Long.valueOf(data[0]));
                newRule.setMoType(data[1]);
                newRule.setAttributeName(data[2]);
                newRule.setAttributeValue(data[3]);
                if (data.length < 5){
                    newRule.setConditions("");
                } else {
                    newRule.setConditions(data[4]);
                }
                if (data.length != 6){
                    newRule.setPriority(null);
                } else {
                    newRule.setPriority(Integer.valueOf(data[5]));
                }

                if (ruleSetMap.containsKey(data[1])) {
                    ruleSetMap.get(data[1]).addAll(new ArrayList<>() {
                        {
                            add(newRule);
                        }
                    });
                } else {
                    ruleSetMap.put(data[1], new ArrayList<>() {
                        {
                            add(newRule);
                        }
                    });
                }
            }
        } catch (final IOException ioe) {
            log.error("Error reading from CSV file {}. {}", fileName, ioe);
        }

        return ruleSetMap;
    }

    private List<AuditResult> getSpecificAuditResults(final List<AuditResult> auditResultList, final AuditStatus status) {
        final List<AuditResult> specificAudits = new ArrayList<>();
        for (final AuditResult auditResult : auditResultList) {
            if (auditResult.getAuditStatus() == status) {
                specificAudits.add(auditResult);
            }
        }
        return specificAudits;
    }

}
