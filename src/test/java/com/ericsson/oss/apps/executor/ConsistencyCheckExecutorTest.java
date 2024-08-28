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

import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_ATTR_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_MO_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_RULES_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.FAILED;
import static com.ericsson.oss.apps.util.MetricConstants.PARTIAL;
import static com.ericsson.oss.apps.util.MetricConstants.SKIPPED;
import static com.ericsson.oss.apps.util.MetricConstants.STATUS;
import static com.ericsson.oss.apps.util.MetricConstants.SUCCEEDED;
import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_CELL_SUB_CAP;
import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_MAX_WRI_PROT;
import static com.ericsson.oss.apps.util.TestDefaults.CFRA_ENABLE;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_JOB_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_RULESET_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION;
import static com.ericsson.oss.apps.util.TestDefaults.EUTRAN_CELL_FDD;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_1_FDN;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_2_FDN;
import static com.ericsson.oss.apps.util.TestDefaults.PRACH_CONFIG_ENABLED;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_ATHLONE;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_ATHLONE_UUID;
import static com.ericsson.oss.apps.util.TestDefaults.VALID_CRON;
import static com.ericsson.oss.apps.util.TestDefaults.ZZZ_TEMPORARY_74;
import static java.util.stream.Collectors.groupingBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.CannotCreateTransactionException;

import com.ericsson.oss.apps.CoreApplication;
import com.ericsson.oss.apps.CoreApplicationTest;
import com.ericsson.oss.apps.model.Execution;
import com.ericsson.oss.apps.model.Job;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.model.Scope;
import com.ericsson.oss.apps.repository.AuditResultsRepository;
import com.ericsson.oss.apps.repository.ExecutionsRepository;
import com.ericsson.oss.apps.repository.ScopeRepository;
import com.ericsson.oss.apps.service.CMService;
import com.ericsson.oss.apps.service.MetricService;
import com.ericsson.oss.apps.service.RetentionService;
import com.ericsson.oss.apps.service.RuleService;
import com.ericsson.oss.apps.service.Services;
import com.ericsson.oss.apps.validation.RuleSetValidator;

/**
 * Unit tests for {@link ConsistencyCheckExecutor} class.
 */
@ActiveProfiles("test")
@AutoConfigureObservability
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = { CoreApplication.class, CoreApplicationTest.class })
public class ConsistencyCheckExecutorTest {
    private final Job job = createDefaultDomainConfigModel();
    private ConsistencyCheckExecutor objectUnderTest;
    @Mock
    private ExecutionsRepository executionsRepository;
    @Mock
    private AuditResultsRepository auditResultsRepository;
    @Mock
    private CMService ncmpService;
    @Mock
    private RuleService ruleService;
    @Mock
    private MetricService metricService;
    @Mock
    private RetentionService retentionService;
    @Mock
    private ScopeRepository scopeRepository;
    @Mock
    private AuditConfig auditConfig;
    @Mock
    private RuleSetValidator ruleSetValidator;
    @Value("${database.retry.scheduleInitiated.maxAttempts}")
    private int maxAttempts;

    private List<Rule> rules;
    private Map<String, List<Rule>> rulesByMoType;
    private Map<String, Map<String, List<Rule>>> fdnToRulesByMoType;

    @BeforeEach
    public void setUp() {
        final Services services = new Services(ncmpService, ruleService, retentionService, metricService, ruleSetValidator);
        objectUnderTest = new ConsistencyCheckExecutor(auditResultsRepository, job, executionsRepository, services,
                scopeRepository, auditConfig);

        final RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(maxAttempts);
        retryTemplate.setRetryPolicy(retryPolicy);
        ConsistencyCheckExecutor.setRetryTemplate(retryTemplate);

        rules = createListOfRules();
        rulesByMoType = createMapOfRulesByMo(rules);
        fdnToRulesByMoType = createFdnToRulesByMoType(createScope(), rulesByMoType);
    }

    private Map<String, Map<String, List<Rule>>> createFdnToRulesByMoType(final Scope scope, final Map<String, List<Rule>> rulesByMoType) {
        final Map<String, Map<String, List<Rule>>> fdnToRulesByMoType = new HashMap<>();
        for (final String fdn : scope.getFdns()) {
            fdnToRulesByMoType.put(fdn, rulesByMoType);
        }

        return fdnToRulesByMoType;
    }

    @Test
    public void whenConsistencyCheckExecutorRuns_thenMethodFlowIsCorrect() throws Exception {
        when(auditConfig.getMoQueueSize()).thenReturn(1000);
        when(auditConfig.getThreadPoolSize()).thenReturn(1);

        when(ruleService.getRulesForRuleset(DEFAULT_RULESET_NAME)).thenReturn(rules);
        when(ruleService.getRulesByMoType(rules)).thenReturn(rulesByMoType);
        when(ruleSetValidator.validateNodeLevelRules(any(Collection.class), any(Map.class))).thenReturn(fdnToRulesByMoType);
        final Optional<Scope> scopeOptional = Optional.of(createScope());
        when(scopeRepository.findByName(SCOPE_ATHLONE)).thenReturn(scopeOptional);
        when(ncmpService.populateEaccManagedObjects(any(), any(BlockingQueue.class))).thenReturn(2);
        when(executionsRepository.save(any(Execution.class))).thenReturn(new Execution());

        objectUnderTest.run();
        final InOrder inOrder = inOrder(metricService, auditResultsRepository, scopeRepository, ncmpService, executionsRepository, ruleService);

        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(SUCCEEDED));
        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(FAILED));
        inOrder.verify(executionsRepository, times(1)).save(any(Execution.class));
        inOrder.verify(ruleService, times(1)).getRulesForRuleset(DEFAULT_RULESET_NAME);
        inOrder.verify(ruleService, times(1)).getRulesByMoType(rules);
        inOrder.verify(scopeRepository, times(1)).findByName(SCOPE_ATHLONE);
        inOrder.verify(ncmpService, times(1)).populateEaccManagedObjects(any(Map.class), any(BlockingQueue.class));
        inOrder.verify(metricService, times(1)).stopTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(SUCCEEDED));
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_TOTAL, STATUS, SUCCEEDED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_MO_TOTAL, 0, STATUS, SUCCEEDED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_RULES_TOTAL, 7, STATUS, SUCCEEDED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_ATTR_TOTAL, 0, STATUS, SUCCEEDED);
        inOrder.verify(executionsRepository, times(1)).save(any(Execution.class));
    }

    @Test
    public void whenConsistencyCheckExecutorRuns_AndThrowsCMServiceException_thenExecutionStatusIsFailed() throws Exception {
        when(auditConfig.getMoQueueSize()).thenReturn(1000);
        when(auditConfig.getThreadPoolSize()).thenReturn(1);

        when(ruleService.getRulesForRuleset(DEFAULT_RULESET_NAME)).thenReturn(rules);
        when(ruleService.getRulesByMoType(rules)).thenReturn(rulesByMoType);
        when(ruleSetValidator.validateNodeLevelRules(any(Collection.class), any(Map.class))).thenReturn(fdnToRulesByMoType);
        final Optional<Scope> scopeOptional = Optional.of(createScope());
        when(scopeRepository.findByName(SCOPE_ATHLONE)).thenReturn(scopeOptional);
        when(ncmpService.populateEaccManagedObjects(any(), any(BlockingQueue.class))).thenReturn(0);
        when(executionsRepository.save(any(Execution.class))).thenReturn(new Execution());

        objectUnderTest.run();
        final InOrder inOrder = inOrder(metricService, auditResultsRepository, scopeRepository, ncmpService, executionsRepository, ruleService);

        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(SUCCEEDED));
        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(FAILED));
        inOrder.verify(executionsRepository, times(1)).save(any(Execution.class));
        inOrder.verify(ruleService, times(1)).getRulesForRuleset(DEFAULT_RULESET_NAME);
        inOrder.verify(ruleService, times(1)).getRulesByMoType(rules);
        inOrder.verify(scopeRepository, times(1)).findByName(SCOPE_ATHLONE);
        inOrder.verify(ncmpService, times(1)).populateEaccManagedObjects(any(Map.class), any(BlockingQueue.class));
        inOrder.verify(metricService, times(1)).stopTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(FAILED));
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_TOTAL, STATUS, FAILED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_MO_TOTAL, 0, STATUS, FAILED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_RULES_TOTAL, 7, STATUS, FAILED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_ATTR_TOTAL, 0, STATUS, FAILED);
        inOrder.verify(executionsRepository, times(1)).save(any(Execution.class));
    }

    @Test
    public void whenConsistencyCheckExecutorRuns_AndSomeAreSuccessful_thenExecutionStatusIsPartial() throws Exception {
        when(auditConfig.getMoQueueSize()).thenReturn(1000);
        when(auditConfig.getThreadPoolSize()).thenReturn(1);

        when(ruleService.getRulesForRuleset(DEFAULT_RULESET_NAME)).thenReturn(rules);
        when(ruleService.getRulesByMoType(rules)).thenReturn(rulesByMoType);
        when(ruleSetValidator.validateNodeLevelRules(any(Collection.class), any(Map.class))).thenReturn(fdnToRulesByMoType);
        final Optional<Scope> scopeOptional = Optional.of(createScope());
        when(scopeRepository.findByName(SCOPE_ATHLONE)).thenReturn(scopeOptional);
        when(ncmpService.populateEaccManagedObjects(any(), any(BlockingQueue.class))).thenReturn(1);
        when(executionsRepository.save(any(Execution.class))).thenReturn(new Execution());
        objectUnderTest.run();
        final InOrder inOrder = inOrder(metricService, auditResultsRepository, scopeRepository, ncmpService, executionsRepository, ruleService);

        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(SUCCEEDED));
        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(PARTIAL));
        inOrder.verify(executionsRepository, times(1)).save(any(Execution.class));
        inOrder.verify(ruleService, times(1)).getRulesForRuleset(DEFAULT_RULESET_NAME);
        inOrder.verify(ruleService, times(1)).getRulesByMoType(rules);
        inOrder.verify(scopeRepository, times(1)).findByName(SCOPE_ATHLONE);
        inOrder.verify(ncmpService, times(1)).populateEaccManagedObjects(any(Map.class), any(BlockingQueue.class));
        inOrder.verify(metricService, times(1)).stopTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(PARTIAL));
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_TOTAL, STATUS, PARTIAL);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_MO_TOTAL, 0, STATUS, PARTIAL);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_RULES_TOTAL, 7, STATUS, PARTIAL);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_ATTR_TOTAL, 0, STATUS, PARTIAL);
        inOrder.verify(executionsRepository, times(1)).save(any(Execution.class));
    }

    @Test
    public void whenConsistencyCheckExecutorRuns_AndAnotherJobIsRunning_thenExecutionStatusSkippedAndSkippedMetricAreUpdated() {
        when(executionsRepository.save(any(Execution.class))).thenReturn(new Execution());
        objectUnderTest.canSetInProgress();

        objectUnderTest.run();

        objectUnderTest.clearAuditInProgress();

        final InOrder inOrder = inOrder(metricService, executionsRepository);
        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(SKIPPED));
        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(SUCCEEDED));
        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(FAILED));
        inOrder.verify(metricService, times(1)).stopTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(SKIPPED));
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_TOTAL, STATUS, SKIPPED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_MO_TOTAL, 0, STATUS, SKIPPED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_RULES_TOTAL, 0, STATUS, SKIPPED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_ATTR_TOTAL, 0, STATUS, SKIPPED);
        verify(executionsRepository, times(1)).save(any(Execution.class));
    }

    @Test
    public void whenPerformAuditThrowsIOException_thenExecutionStatusAndFailedMetricAreUpdated() {
        when(ruleService.getRulesForRuleset(DEFAULT_RULESET_NAME)).thenThrow(RuntimeException.class);
        when(executionsRepository.save(any(Execution.class))).thenReturn(new Execution());
        objectUnderTest.run();

        final InOrder inOrder = inOrder(metricService, executionsRepository);
        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(SUCCEEDED));
        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(FAILED));
        inOrder.verify(metricService, times(1)).stopTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(FAILED));
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_TOTAL, STATUS, FAILED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_MO_TOTAL, 0, STATUS, FAILED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_RULES_TOTAL, 0, STATUS, FAILED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_ATTR_TOTAL, 0, STATUS, FAILED);
        verify(executionsRepository, times(2)).save(any(Execution.class));
    }

    @Test
    public void whenConsistencyCheckExecutorRuns_AndEncountersDataAccessExceptionToFailure_thenExecutionStatusAndFailedMetricAreUpdated() {
        when(ruleService.getRulesForRuleset(DEFAULT_RULESET_NAME)).thenThrow(DataAccessResourceFailureException.class);
        when(executionsRepository.save(any(Execution.class))).thenReturn(new Execution());
        objectUnderTest.run();

        final InOrder inOrder = inOrder(metricService, executionsRepository);
        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(SUCCEEDED));
        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(FAILED));
        inOrder.verify(metricService, times(1)).stopTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(FAILED));
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_TOTAL, STATUS, FAILED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_MO_TOTAL, 0, STATUS, FAILED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_RULES_TOTAL, 0, STATUS, FAILED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_ATTR_TOTAL, 0, STATUS, FAILED);
        verify(executionsRepository, times(maxAttempts + 1)).save(any(Execution.class));
    }

    @Test
    public void whenConsistencyCheckExecutorRuns_AndEncountersTransactionExceptionToFailure_thenExecutionStatusAndFailedMetricAreUpdated() {
        when(ruleService.getRulesForRuleset(DEFAULT_RULESET_NAME)).thenThrow(CannotCreateTransactionException.class);
        when(executionsRepository.save(any(Execution.class))).thenReturn(new Execution());
        objectUnderTest.run();

        final InOrder inOrder = inOrder(metricService, executionsRepository);
        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(SUCCEEDED));
        inOrder.verify(metricService, times(1)).startTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(FAILED));
        inOrder.verify(metricService, times(1)).stopTimer(any(String.class), eq(EACC_EXECUTIONS_DURATION_SECONDS), eq(STATUS), eq(FAILED));
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_TOTAL, STATUS, FAILED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_MO_TOTAL, 0, STATUS, FAILED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_RULES_TOTAL, 0, STATUS, FAILED);
        inOrder.verify(metricService, times(1)).increment(EACC_EXECUTIONS_ATTR_TOTAL, 0, STATUS, FAILED);
        verify(executionsRepository, times(maxAttempts + 1)).save(any(Execution.class));
    }

    @Test
    void whenGetScopeFdnsIsCalled_AndScopeNoDuplicateFdns_thenTheSameNumberOfFdnsAreReturned()
            throws Exception {
        //expectations
        final Scope scope = createScope();
        final Optional<Scope> scopeOptional = Optional.of(scope);
        when(scopeRepository.findByName(SCOPE_ATHLONE)).thenReturn(scopeOptional);

        //execute
        final List<String> uniqueFdns = objectUnderTest.getScopeFdns(scope.getName());

        //verification
        assertThat(scope.getFdns()).hasSize(2);
        assertThat(uniqueFdns).hasSize(2);
    }

    @Test
    void whenGetScopeFdnsIsCalled_AndScopeHasDuplicateFdns_thenUniqueNumberOfFdnsAreReturned()
            throws Exception {
        //expectations
        final Scope scope = createScopeWithDuplicates();
        final Optional<Scope> scopeOptional = Optional.of(scope);
        when(scopeRepository.findByName(SCOPE_ATHLONE)).thenReturn(scopeOptional);

        //execute
        final List<String> uniqueFdns = objectUnderTest.getScopeFdns(scope.getName());

        //verification
        assertThat(scope.getFdns()).hasSize(3);
        assertThat(uniqueFdns).hasSize(2);
    }

    @Test
    void whenGetScopeFdnsIsCalled_AndScopeEmpty_thenZeroFdnsAreReturned()
            throws Exception {
        //expectations
        final Scope scope = createEmptyScope();
        final Optional<Scope> scopeOptional = Optional.empty();
        when(scopeRepository.findByName(SCOPE_ATHLONE)).thenReturn(scopeOptional);

        //execute
        final List<String> uniqueFdns = objectUnderTest.getScopeFdns(scope.getName());

        //verification
        assertThat(scope.getFdns()).isEmpty();
        assertThat(uniqueFdns).isEmpty();
    }

    private List<Rule> createListOfRules() {
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(ENODE_BFUNCTION, PRACH_CONFIG_ENABLED, "true"));
        rules.add(new Rule(ENODE_BFUNCTION, ZZZ_TEMPORARY_74, "-2000000000", ""));
        rules.add(new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000"));
        rules.add(new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "true", ""));
        rules.add(new Rule(EUTRAN_CELL_FDD, CFRA_ENABLE, "true"));
        rules.add(new Rule(EUTRAN_CELL_FDD, "commonCqiPeriodicity", "80", ""));
        rules.add(new Rule(EUTRAN_CELL_FDD, "ulVolteCovMobThr", "5"));
        return rules;
    }

    private Map<String, List<Rule>> createMapOfRulesByMo(final List<Rule> rules) {
        return rules.stream().collect(groupingBy(Rule::getMoType));
    }

    private Job createDefaultDomainConfigModel() {
        final Job config = new Job();
        config.setId(1L);
        config.setJobName(DEFAULT_JOB_NAME);
        config.setScopeName(SCOPE_ATHLONE);
        config.setSchedule(VALID_CRON);
        config.setRuleSetName(DEFAULT_RULESET_NAME);
        return config;
    }

    private Scope createScope() {
        final Scope scope = new Scope();
        scope.setName(SCOPE_ATHLONE);
        scope.setId(SCOPE_ATHLONE_UUID);
        scope.setFdns(Arrays.asList(MANAGED_ELEMENT_1_FDN, MANAGED_ELEMENT_2_FDN));
        return scope;
    }

    private Scope createScopeWithDuplicates() {
        final Scope scope = new Scope();
        scope.setName(SCOPE_ATHLONE);
        scope.setId(SCOPE_ATHLONE_UUID);
        scope.setFdns(Arrays.asList(MANAGED_ELEMENT_1_FDN, MANAGED_ELEMENT_2_FDN, MANAGED_ELEMENT_1_FDN));
        return scope;
    }

    private Scope createEmptyScope() {
        final Scope scope = new Scope();
        scope.setName(SCOPE_ATHLONE);
        scope.setId(SCOPE_ATHLONE_UUID);
        scope.setFdns(Collections.emptyList());
        return scope;
    }
}
