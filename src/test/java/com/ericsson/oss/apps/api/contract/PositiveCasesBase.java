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

package com.ericsson.oss.apps.api.contract;

import static com.ericsson.oss.apps.executor.ExecutionType.OPEN_LOOP;
import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_COMPLETE;
import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_CELL_SUB_CAP;
import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_MAX_WRI_PROT;
import static com.ericsson.oss.apps.util.TestDefaults.CFRA_ENABLE;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_JOB_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_RULESET_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_SCOPE_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION;
import static com.ericsson.oss.apps.util.TestDefaults.EUTRAN_CELL_FDD;
import static com.ericsson.oss.apps.util.TestDefaults.RULESET_ID_EXAMPLE;
import static com.ericsson.oss.apps.util.TestDefaults.RULESET_URI_EXAMPLE;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_ATHLONE;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_ATHLONE_ID;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_ATHLONE_UUID;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_DUBLIN;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_DUBLIN_ID;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_URI_ATHLONE_EXAMPLE;
import static com.ericsson.oss.apps.util.TestDefaults.SCOPE_URI_DUBLIN_EXAMPLE;
import static com.ericsson.oss.apps.util.TestDefaults.VALID_CRON;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.ericsson.oss.apps.util.ValidationObject;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.apps.api.model.EaccAuditResult;
import com.ericsson.oss.apps.api.model.EaccExecution;
import com.ericsson.oss.apps.api.model.EaccJob;
import com.ericsson.oss.apps.api.model.EaccPaginatedAuditResults;
import com.ericsson.oss.apps.api.model.EaccRulesetMetadata;
import com.ericsson.oss.apps.api.model.EaccScopeMetadata;
import com.ericsson.oss.apps.executor.AuditStatus;
import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.executor.ExecutionStatus;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.model.RuleSet;
import com.ericsson.oss.apps.model.Scope;
import com.ericsson.oss.apps.repository.RuleSetRepository;
import com.ericsson.oss.apps.service.CMServiceException;
import com.ericsson.oss.apps.service.ExecutionService;
import com.ericsson.oss.apps.service.JobService;
import com.ericsson.oss.apps.service.ModelDiscoveryService;
import com.ericsson.oss.apps.service.RuleService;
import com.ericsson.oss.apps.service.RuleSetService;
import com.ericsson.oss.apps.service.ScopeService;
import com.ericsson.oss.apps.service.ValidationService;
import com.ericsson.oss.apps.util.AuditLogInterceptor;
import com.ericsson.oss.apps.validation.ModelManagerImpl;
import com.ericsson.oss.apps.validation.RuleValidationException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.opencsv.exceptions.CsvValidationException;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles({ "test", "contract" })
@SpringBootTest
public class PositiveCasesBase {

    private static final boolean IS_VALIDATED = true;
    private static final ValidationObject VALIDATION_SUCCESS = new ValidationObject(IS_VALIDATED, EMPTY_STRING, VALIDATION_COMPLETE, HttpStatus.OK);
    private static final String RANDOM_DATE = "2023-03-13T15:18:44.033Z";
    private static final String SAMPLE_RULESET = "sample_ruleset";
    private static final String SAMPLE_RULESET_ONE = "sample_ruleset_one";
    private static final String MANAGED_ELEMENT_FDN = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030";
    private static final String GNBCUCP_FUNCTION_FDN = MANAGED_ELEMENT_FDN + ",GNBCUCPFunction=1";
    private static final String NR_CELL_CU_FDN = GNBCUCP_FUNCTION_FDN + ",NRCellCU=NR03gNodeBRadio00030-4";
    private static final String NR_CELL_DU_FDN = MANAGED_ELEMENT_FDN + ",GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4";
    private static final String PARTIAL_NR_CELL_DU_FDN = "NRCellDU=NR03gNodeBRadio00030-4";
    private static final String PARTIAL_FDN_LEFT_LIKE = "%NRCellCU=NR03gNodeBRadio00030-4";
    private static final String PARTIAL_FDN_RIGHT_LIKE = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,%";
    private static final String NR_CELL_DU = "NRCellDU";
    private static final String EXECUTION_ID_ONE = "1";
    private static final String ALL_IRELAND_2 = "all-ireland-2";
    private static final String ALL_IRELAND_4 = "all-ireland-4";
    private static final String CREATED_SCOPE_NAME = "westmeath_10-07-2023";
    /* This is hardcoded to 2000 for now in the client so faking it here for now */
    private static final int PAGE_SIZE = 2000;

    @Autowired
    private WebApplicationContext context;
    @MockBean
    private RuleSetService ruleSetService;
    @MockBean
    private RuleService ruleService;
    @MockBean
    private JobService jobService;
    @MockBean
    private RuleSetRepository ruleSetRepository;
    @MockBean
    private ExecutionService executionService;
    @MockBean
    private ScopeService scopeService;
    @MockBean
    private ValidationService validationService;
    @MockBean
    ModelDiscoveryService modelDiscoveryService;
    @MockBean
    ModelManagerImpl modelManager;
    @MockBean
    AuditLogInterceptor auditLogInterceptor;

    @BeforeEach
    public void setup() throws CsvRequiredFieldEmptyException, CsvValidationException, IOException, CMServiceException, RuleValidationException {
        final EaccExecution execution1 = createEaccExecution(EXECUTION_ID_ONE, ExecutionStatus.AUDIT_SUCCESSFUL, 1, 12, 4);
        final EaccExecution execution2 = createEaccExecution("2", ExecutionStatus.AUDIT_IN_PROGRESS, 10, 1, 8);
        final EaccExecution execution3 = createEaccExecution("3", ExecutionStatus.AUDIT_FAILED, 40, 20, 66);
        final EaccExecution execution4 = createEaccExecution("4", ExecutionStatus.AUDIT_IN_PROGRESS, 1, 1, 4);

        final EaccJob job1 = createEaccJob(DEFAULT_JOB_NAME, DEFAULT_RULESET_NAME, VALID_CRON, DEFAULT_SCOPE_NAME);
        final EaccJob job2 = createEaccJob(ALL_IRELAND_2, DEFAULT_RULESET_NAME, VALID_CRON, DEFAULT_SCOPE_NAME);
        final EaccJob job4 = createEaccJob(ALL_IRELAND_4, SAMPLE_RULESET, VALID_CRON, SCOPE_ATHLONE);
        final EaccJob job1Updated = createEaccJob(DEFAULT_JOB_NAME, SAMPLE_RULESET, VALID_CRON, SCOPE_ATHLONE);

        final EaccAuditResult auditResult1 = createEaccAuditResult("1", NR_CELL_DU_FDN, NR_CELL_DU,
                "csiRsShiftingPrimary", "DEACTIVATED", "DEACTIVATED",
                AuditStatus.CONSISTENT, "16", ChangeStatus.NOT_APPLIED);
        final EaccAuditResult auditResult2 = createEaccAuditResult("2", NR_CELL_DU_FDN, NR_CELL_DU,
                "subCarrierSpacing", "120", "110",
                AuditStatus.INCONSISTENT, "17", ChangeStatus.IMPLEMENTATION_IN_PROGRESS);
        final EaccAuditResult auditResult3 = createEaccAuditResult("3", NR_CELL_CU_FDN, "NRCellCU",
                "mcpcPSCellEnabled", "false", "true",
                AuditStatus.INCONSISTENT, "14", ChangeStatus.IMPLEMENTATION_FAILED);
        final EaccAuditResult auditResult4 = createEaccAuditResult("4", GNBCUCP_FUNCTION_FDN, "GNBCUCPFunction",
                "maxNgRetryTime", "30", "20",
                AuditStatus.INCONSISTENT, "12", ChangeStatus.IMPLEMENTATION_COMPLETE);
        final EaccAuditResult auditResult5 = createEaccAuditResult("5", GNBCUCP_FUNCTION_FDN, "GNBCUCPFunction",
                "endpointResDepHEnabled", "true", "false",
                AuditStatus.INCONSISTENT, "18", ChangeStatus.IMPLEMENTATION_COMPLETE);

        mockRulesetService();

        mockScopeService();

        when(validationService.validateJobNameAndJobExists(job1.getJobName())).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateJobAndThatJobNameDoesNotExist(job1)).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateJobAndThatJobNameDoesNotExist(job4)).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateJobNameAndJobExists(job2.getJobName())).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateScopeName(SCOPE_ATHLONE)).thenCallRealMethod();
        when(validationService.validateScopeName(CREATED_SCOPE_NAME)).thenCallRealMethod();
        when(validationService.validateExecutionIdFormat(execution1.getId())).thenCallRealMethod();
        when(validationService.validatePagination(any(), any())).thenCallRealMethod();
        when(validationService.validateFilter(any())).thenCallRealMethod();

        when(executionService.isJobInUse(job1.getJobName())).thenReturn(false);
        when(validationService.validateRulesetNameAndExists(job1.getRulesetName())).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateScope(job1.getScopeName())).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateScope(SCOPE_ATHLONE)).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateRulesetNameAndExists(SAMPLE_RULESET)).thenReturn(VALIDATION_SUCCESS);
        when(modelManager.isModelValidationReady()).thenReturn(Boolean.TRUE);
        when(jobService.update(job1Updated)).thenReturn(job1Updated);

        when(jobService.create(job1)).thenReturn(job1);
        when(jobService.create(job2)).thenReturn(job2);
        when(jobService.create(job4)).thenReturn(job4);
        when(jobService.getAllJobs()).thenReturn(Arrays.asList(job1, job2));

        when(jobService.existsByName(DEFAULT_JOB_NAME)).thenReturn(false);
        when(jobService.existsByName(ALL_IRELAND_2)).thenReturn(true);

        when(jobService.isRuleSetInUse(DEFAULT_RULESET_NAME)).thenReturn(false);
        when(jobService.isScopeInUse(SCOPE_ATHLONE)).thenReturn(false);

        when(executionService.getAllExecutions()).thenReturn(Arrays.asList(execution1, execution2, execution3, execution4));
        when(executionService.getExecutionByJobName(DEFAULT_JOB_NAME)).thenReturn(Collections.singletonList(execution1));

        when(executionService.existsByExecutionId(EXECUTION_ID_ONE)).thenReturn(true);

        when(executionService.getAuditResults(EXECUTION_ID_ONE, null, null, Collections.emptyList()))
                .thenReturn(createEaccPaginatedAuditResult(5L, 1, null, null, false, false, Arrays.asList(auditResult1,
                        auditResult2, auditResult3, auditResult4, auditResult5)));
        when(executionService.getAuditResults(EXECUTION_ID_ONE, 0, PAGE_SIZE, Collections.emptyList()))
                .thenReturn(createEaccPaginatedAuditResult(5L, 2, PAGE_SIZE, 0, false, true, Arrays.asList(auditResult1,
                        auditResult2, auditResult3, auditResult4)));
        when(executionService.getAuditResults(EXECUTION_ID_ONE, 1, PAGE_SIZE, Collections.emptyList()))
                .thenReturn(createEaccPaginatedAuditResult(5L, 2, PAGE_SIZE, 1, true, false, Arrays.asList(auditResult5)));
        when(executionService.getAuditResults(EXECUTION_ID_ONE, 0, PAGE_SIZE, Collections.singletonList("auditStatus:Inconsistent")))
                .thenReturn(createEaccPaginatedAuditResult(4L, 1, PAGE_SIZE, 0, false, false, Arrays.asList(auditResult2,
                        auditResult3, auditResult4, auditResult5)));
        when(executionService.getAuditResults(EXECUTION_ID_ONE, 0, PAGE_SIZE,
                List.of("changeStatus:(Implementation in progress,Implementation complete,Implementation failed,Implementation aborted,Reversion in progress,Reversion complete,Reversion failed,Reversion aborted)")))
                        .thenReturn(createEaccPaginatedAuditResult(4L, 2, PAGE_SIZE, 0, false, true,
                                Arrays.asList(auditResult2,
                                        auditResult3, auditResult4)));
        when(executionService.getAuditResults(EXECUTION_ID_ONE, 1, PAGE_SIZE,
                List.of("changeStatus:(Implementation in progress,Implementation complete,Implementation failed,Implementation aborted,Reversion in progress,Reversion complete,Reversion failed,Reversion aborted)")))
                        .thenReturn(createEaccPaginatedAuditResult(4L, 2, PAGE_SIZE, 1, true, false,
                                Arrays.asList(auditResult5)));
        when(executionService.getAuditResults(EXECUTION_ID_ONE, 0, 1,
                List.of("changeStatus:(Implementation in progress,Reversion in progress)")))
                .thenReturn(createEaccPaginatedAuditResult(0L, 0, 1, 0, false, false,
                        Collections.emptyList()));
        when(executionService.getAuditResults(EXECUTION_ID_ONE, 0, PAGE_SIZE,
                List.of("managedObjectFdn:" + NR_CELL_DU_FDN)))
                        .thenReturn(createEaccPaginatedAuditResult(1L, 1, PAGE_SIZE, 0, false, false, Arrays.asList(auditResult2)));
        when(executionService.getAuditResults(EXECUTION_ID_ONE, 0, PAGE_SIZE,
                List.of("managedObjectFdn:%" + PARTIAL_NR_CELL_DU_FDN + "%")))
                        .thenReturn(createEaccPaginatedAuditResult(2L, 1, PAGE_SIZE, 0, false, false, Arrays.asList(auditResult1, auditResult2)));

        when(executionService.getAuditResults(EXECUTION_ID_ONE, 0, PAGE_SIZE,
                List.of("managedObjectFdn:" + PARTIAL_FDN_LEFT_LIKE)))
                        .thenReturn(createEaccPaginatedAuditResult(1L, 1, PAGE_SIZE, 0, false, false, Arrays.asList(auditResult3)));

        when(executionService.getAuditResults(EXECUTION_ID_ONE, 0, PAGE_SIZE,
                List.of("managedObjectFdn:" + PARTIAL_FDN_RIGHT_LIKE)))
                        .thenReturn(createEaccPaginatedAuditResult(3L, 1, PAGE_SIZE, 0, false, false,
                                Arrays.asList(auditResult1, auditResult2, auditResult3)));
        when(executionService.getAuditResults(EXECUTION_ID_ONE, 0, PAGE_SIZE,
                List.of("auditStatus:Inconsistent",
                        "managedObjectFdn:SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030,GNBDUFunction=1,NRCellDU=NR03gNodeBRadio00030-4")))
                                .thenReturn(createEaccPaginatedAuditResult(1L, 1, PAGE_SIZE, 0, false, false, Arrays.asList(auditResult2)));
        when(executionService.getAuditResults(EXECUTION_ID_ONE, 0, PAGE_SIZE,
                List.of("auditStatus:Inconsistent", "managedObjectFdn:%NR03gNodeBRadio00030-4%")))
                        .thenReturn(createEaccPaginatedAuditResult(2L, 1, PAGE_SIZE, 0, false, false,
                                Arrays.asList(auditResult2, auditResult3)));
        when(auditLogInterceptor.preHandle(any(), any(), any())).thenReturn(true);

        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private void mockRulesetService() throws CsvRequiredFieldEmptyException, IOException, RuleValidationException {
        final EaccRulesetMetadata eaccRulesetMetadata = new EaccRulesetMetadata();
        eaccRulesetMetadata.setUri(RULESET_URI_EXAMPLE);
        eaccRulesetMetadata.setId(RULESET_ID_EXAMPLE);
        eaccRulesetMetadata.setRulesetName(SAMPLE_RULESET);

        final EaccRulesetMetadata eaccRulesetOneMetadata = new EaccRulesetMetadata();
        eaccRulesetOneMetadata.setUri(RULESET_URI_EXAMPLE);
        eaccRulesetOneMetadata.setId(RULESET_ID_EXAMPLE);
        eaccRulesetOneMetadata.setRulesetName(SAMPLE_RULESET_ONE);

        final RuleSet ruleset = new RuleSet(SAMPLE_RULESET);
        final Rule rule1 = new Rule(ENODE_BFUNCTION, "forcedSiTunnelingActive", "true", "");
        final Rule rule2 = new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000");
        final Rule rule3 = new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_MAX_WRI_PROT, "true", "");
        final Rule rule4 = new Rule(EUTRAN_CELL_FDD, CFRA_ENABLE, "true", "");
        final List<Rule> rules = new ArrayList<>(Arrays.asList(rule1, rule2, rule3, rule4));
        ruleset.setRules(rules);

        final RuleSet rulesetOne = ruleset;

        final RuleSet updatedRuleset = new RuleSet("SampleRuleset");
        final List<Rule> updatedRules = new ArrayList<>(List.of(rule1));
        updatedRuleset.setRules(updatedRules);

        final ByteArrayResource csvByteArray = new ByteArrayResource(
                ("moType,attributeName,attributeValue\n" +
                        "ENodeBFunction,prachConfigEnabled,true\n").getBytes());

        final UUID uuid = UUID.fromString("3f2504e0-4f89-11d3-9a0c-0305e82c3301");

        //TC: Positive flow
        //Post valid Ruleset
        when(ruleSetService.createRulesetFromCsv(eq(SAMPLE_RULESET), any(MultipartFile.class))).thenReturn(ruleset);
        when(ruleSetService.createRulesetFromCsv(eq(SAMPLE_RULESET_ONE), any(MultipartFile.class))).thenReturn(rulesetOne);
        when(ruleSetService.createEaccRulesetMetadata(ruleset)).thenReturn(eaccRulesetMetadata);
        when(ruleSetService.createEaccRulesetMetadata(rulesetOne)).thenReturn(eaccRulesetOneMetadata);
        when(validationService.validateRulesetName(SAMPLE_RULESET)).thenCallRealMethod();
        when(validationService.validateRulesetName(SAMPLE_RULESET_ONE)).thenCallRealMethod();
        when(ruleSetService.existsByName(SAMPLE_RULESET)).thenReturn(false);
        when(modelManager.isModelValidationReady()).thenReturn(Boolean.TRUE);

        //Validate User Input
        when(ruleSetService.existsByName(DEFAULT_RULESET_NAME)).thenReturn(true);
        //Get all Rulesets
        when(ruleSetService.getAllRulesetMetadata()).thenReturn(Collections.singletonList(eaccRulesetMetadata));

        //Get Ruleset 3f2504e0-4f89-11d3-9a0c-0305e82c3301
        when(ruleSetService.existsById(uuid)).thenReturn(true);
        when(ruleService.getRulesForRulesetId(uuid)).thenReturn(rules);
        when(ruleSetService.createCsvFromRuleset(uuid)).thenReturn(csvByteArray);

        //Delete Ruleset
        when(ruleSetService.getRulesetNameFromId(any(UUID.class))).thenReturn(SAMPLE_RULESET);
        doNothing().when(ruleSetService).deleteRulesetById(any(UUID.class));

        //Put New Ruleset with id 3f2504e0-4f89-11d3-9a0c-0305e82c3301
        when(ruleSetService.updateRulesetFromCsv(eq(uuid), any(MultipartFile.class))).thenReturn(updatedRuleset);
        when(ruleSetService.createEaccRulesetMetadata(updatedRuleset)).thenReturn(eaccRulesetMetadata);

    }

    private void mockScopeService() throws CsvRequiredFieldEmptyException, CsvValidationException, IOException {
        final EaccScopeMetadata scopeAthlone = new EaccScopeMetadata();
        scopeAthlone.setId(SCOPE_ATHLONE_ID);
        scopeAthlone.setScopeName(SCOPE_ATHLONE);
        scopeAthlone.setUri(SCOPE_URI_ATHLONE_EXAMPLE);
        final EaccScopeMetadata scopeDublin = new EaccScopeMetadata();
        scopeDublin.setId(SCOPE_DUBLIN_ID);
        scopeDublin.setScopeName(SCOPE_DUBLIN);
        scopeDublin.setUri(SCOPE_URI_DUBLIN_EXAMPLE);

        final Scope createdScope = new Scope();
        createdScope.setId(UUID.fromString(SCOPE_ATHLONE_ID));
        createdScope.setName(CREATED_SCOPE_NAME);

        //Get all Scopes
        when(scopeService.getAllScopeMetadata()).thenReturn(Arrays.asList(scopeAthlone, scopeDublin));

        //Create scope
        when(scopeService.createScopeFromCsv(eq(CREATED_SCOPE_NAME), any(MultipartFile.class))).thenReturn(createdScope);

        when(jobService.isScopeInUse(SCOPE_ATHLONE)).thenReturn(false);
        when(scopeService.updateScope(eq(createdScope), any(MultipartFile.class))).thenReturn(scopeAthlone);

        //validate user input
        when(scopeService.existsByName(DEFAULT_SCOPE_NAME)).thenReturn(true);

        when(scopeService.getScopeFromUUID(SCOPE_ATHLONE_UUID)).thenReturn(createdScope);
        final ByteArrayResource scopeCsvByteArray = new ByteArrayResource(("fdn\n" +
                "\"SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,ManagedElement=NR03gNodeBRadio00030\"\n" +
                "\"SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00031,ManagedElement=NR03gNodeBRadio00031\"\n" +
                "\"SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00032,ManagedElement=NR03gNodeBRadio00032\"\n").getBytes());
        when(scopeService.getCsvForScope(SCOPE_ATHLONE_UUID)).thenReturn(scopeCsvByteArray);
    }

    private EaccJob createEaccJob(final String jobName, final String rulesetName, final String schedule, final String scopeName) {
        final EaccJob job = new EaccJob();
        job.setJobName(jobName);
        job.setRulesetName(rulesetName);
        job.setSchedule(schedule);
        job.setScopeName(scopeName);
        return job;
    }

    private EaccExecution createEaccExecution(final String id, final ExecutionStatus status, final int cells,
            final int attributes, final int inconsistencies) {
        final long duration = Long.parseLong(id);
        final OffsetDateTime startTime = OffsetDateTime.parse(RANDOM_DATE).plusDays(duration);
        final OffsetDateTime endTime = OffsetDateTime.parse(RANDOM_DATE).plusDays(duration).plusMinutes(5);

        final EaccExecution execution = new EaccExecution();
        execution.setId(id);
        execution.setJobName("all-ireland-".concat(id));
        execution.setExecutionType(OPEN_LOOP.toString());
        execution.setExecutionStartedAt(startTime);
        execution.setExecutionEndedAt(endTime);
        execution.setConsistencyAuditStartedAt(startTime);
        execution.setConsistencyAuditEndedAt(endTime);
        execution.setExecutionStatus(status.toString());
        execution.setTotalMosAudited(cells);
        execution.setTotalAttributesAudited(attributes);
        execution.setInconsistenciesIdentified(inconsistencies);
        return execution;
    }

    private EaccAuditResult createEaccAuditResult(final String id, final String fdn, final String moType,
            final String attributeName, final String currentVale,
            final String preferredValue, final AuditStatus status,
            final String ruleId, final ChangeStatus changeStatus) {
        final EaccAuditResult auditResult = new EaccAuditResult();
        auditResult.setId(id);
        auditResult.setManagedObjectFdn(fdn);
        auditResult.setManagedObjectType(moType);
        auditResult.setAttributeName(attributeName);
        auditResult.setCurrentValue(currentVale);
        auditResult.setPreferredValue(preferredValue);
        auditResult.setAuditStatus(status.toString());
        auditResult.setExecutionId(EXECUTION_ID_ONE);
        auditResult.setRuleId(ruleId);
        if (changeStatus == null) {
            auditResult.setChangeStatus("");
        } else {
            auditResult.setChangeStatus(changeStatus.toString());
        }
        return auditResult;
    }

    private EaccPaginatedAuditResults createEaccPaginatedAuditResult(final Long totalElements, final Integer totalPages,
            final Integer perPage,
            final Integer currentPage, final Boolean hasPrev, final Boolean hasNext,
            final List<EaccAuditResult> auditResults) {
        final EaccPaginatedAuditResults eaccPaginatedAuditResults = new EaccPaginatedAuditResults();
        eaccPaginatedAuditResults.setTotalElements(totalElements);
        eaccPaginatedAuditResults.setTotalPages(totalPages);
        eaccPaginatedAuditResults.setPerPage(perPage);
        eaccPaginatedAuditResults.setCurrentPage(currentPage);
        eaccPaginatedAuditResults.setHasPrev(hasPrev);
        eaccPaginatedAuditResults.setHasNext(hasNext);
        eaccPaginatedAuditResults.results(auditResults);
        return eaccPaginatedAuditResults;
    }

}
