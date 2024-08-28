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


import static com.ericsson.oss.apps.util.Constants.CHANGES_IN_PROGRESS;
import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static com.ericsson.oss.apps.util.Constants.NONEXISTENT_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.PREVIOUS_JOB_EXECUTION;
import static com.ericsson.oss.apps.util.Constants.PROPOSED_IDS_DONT_EXIST;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_COMPLETE;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.TestDefaults.CELL_CAP_MIN_CELL_SUB_CAP;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_UUID;
import static com.ericsson.oss.apps.util.TestDefaults.ENODE_BFUNCTION;
import static com.ericsson.oss.apps.util.TestDefaults.EUTRAN_CELL_FDD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.tomcat.util.http.fileupload.impl.SizeLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.apps.api.model.EaccApprovedAuditResults;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.model.Scope;
import com.ericsson.oss.apps.service.ExecutionService;
import com.ericsson.oss.apps.service.JobService;
import com.ericsson.oss.apps.service.ModelDiscoveryService;
import com.ericsson.oss.apps.service.RuleService;
import com.ericsson.oss.apps.service.RuleSetService;
import com.ericsson.oss.apps.service.ScopeService;
import com.ericsson.oss.apps.service.ValidationService;
import com.ericsson.oss.apps.service.exception.ChangesInProgressException;
import com.ericsson.oss.apps.service.exception.InconsistencyProcessingFailedException;
import com.ericsson.oss.apps.service.exception.UnsupportedOperationException;
import com.ericsson.oss.apps.service.exception.ProposedIdsNotFoundException;
import com.ericsson.oss.apps.util.AuditLogInterceptor;
import com.ericsson.oss.apps.util.ValidationObject;
import com.ericsson.oss.apps.validation.ModelManagerImpl;
import com.ericsson.oss.apps.validation.RuleValidationError;
import com.ericsson.oss.apps.validation.RuleValidationException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.opencsv.exceptions.CsvValidationException;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

@ActiveProfiles({ "test", "contract" })
@SpringBootTest
public class NegativeCasesBase {

    private static final boolean IS_VALIDATED = true;
    private static final ValidationObject VALIDATION_SUCCESS = new ValidationObject(IS_VALIDATED, EMPTY_STRING, VALIDATION_COMPLETE, HttpStatus.OK);
    private static final String EXCEEDS_THE_CONFIGURED_MAXIMUM = "the request was rejected because its size (21577375) exceeds the " +
            "configured maximum (20971520)";
    private static final String EXECUTION_IN_PROGRESS_WITH_JOB_NAME = "execution_in_progress_with_job_name";

    private static final String EXECUTION_ID_ONE = "1";
    private static final String EXECUTION_ID_TWO = "2";
    private static final String EXECUTION_ID_THREE = "3";
    private static final String EXECUTION_ID_FOUR = "4";
    private static final String EXECUTION_ID_NOT_EXISTING = "999";

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private RuleService ruleService;

    @MockBean
    private RuleSetService ruleSetService;

    @MockBean
    public JobService jobService;

    @MockBean
    public ScopeService scopeService;

    @MockBean
    public ExecutionService executionService;

    @MockBean
    private ValidationService validationService;

    @MockBean
    ModelDiscoveryService modelDiscoveryService;

    @MockBean
    ModelManagerImpl modelManager;

    @Mock
    private MaxUploadSizeExceededException maxUploadSizeExceededException;

    @Mock
    private IllegalStateException illegalStateException;
    @Mock
    private SizeLimitExceededException sizeLimitExceededException;
    @MockBean
    AuditLogInterceptor auditLogInterceptor;

    @BeforeEach
    public void setup() throws CsvRequiredFieldEmptyException, IOException, CsvValidationException, RuleValidationException, ChangesInProgressException, UnsupportedOperationException, ProposedIdsNotFoundException, InconsistencyProcessingFailedException {
        when(validationService.validateExecutionIdFormat(nullable(String.class))).thenCallRealMethod();
        when(validationService.validateRulesetName(nullable(String.class))).thenCallRealMethod();
        when(validationService.validateScopeName(nullable(String.class))).thenCallRealMethod();
        mockRulesetService();
        mockScopeService();
        mockJobService();
        mockRevertTestCases();
        mockApplyTestCases();
        mockAuditResultTestCases();
        when(auditLogInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    private void mockJobService() {
        final ValidationObject nonExistingJobNameValidationFailure = new ValidationObject(!IS_VALIDATED, NONEXISTENT_JOBNAME_ERROR,
                VALIDATION_FAILED,
                NOT_FOUND);

        // Mocks for put job
        when(validationService.validateJobNameAndJobExists("INVALID_JOB_NAME")).thenCallRealMethod();
        when(validationService.validateJobNameAndJobExists("internal_error_job_name"))
                .thenThrow(new DataAccessResourceFailureException("Some data access failure"));
        when(validationService.validateJobNameAndJobExists("non_existing_job_name")).thenReturn(nonExistingJobNameValidationFailure);
        when(validationService.validateJobNameAndJobExists(EXECUTION_IN_PROGRESS_WITH_JOB_NAME)).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateRulesetNameAndExists("test_ruleset")).thenReturn(VALIDATION_SUCCESS);
        when(validationService.validateScope("test_scope")).thenReturn(VALIDATION_SUCCESS);
        when(modelManager.isModelValidationReady()).thenReturn(Boolean.TRUE);
        when(executionService.isJobInUse(EXECUTION_IN_PROGRESS_WITH_JOB_NAME)).thenReturn(true);
    }

    private void mockRulesetService() throws IOException, CsvRequiredFieldEmptyException, RuleValidationException {
        final List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(ENODE_BFUNCTION, "forcedSiTunnelingActive", "true"));
        rules.add(new Rule(EUTRAN_CELL_FDD, CELL_CAP_MIN_CELL_SUB_CAP, "1000", ""));
        final List<Rule> emptyRulesList = new ArrayList<>();

        final UUID uuid = UUID.fromString("99ea562f-87cc-424d-9835-f569b15b8eef");
        final UUID uuidIoException = UUID.fromString("270994ce-249a-4a9b-a87d-2f1c4df0bf94");

        //TC: Negative flow
        //Duplicate Ruleset name
        when(ruleSetService.existsByName("duplicate_ruleset")).thenReturn(true);
        //Ruleset containing a null value field
        when(ruleSetService.createRulesetFromCsv(eq("null_value_ruleset"), any(MultipartFile.class)))
                .thenThrow(new CsvRequiredFieldEmptyException());
        //Using a Ruleset that does not exist
        when(ruleSetService.createRulesetFromCsv(eq("non_existent_ruleset"), any(MultipartFile.class)))
                .thenThrow(new IOException());

        when(ruleSetService.createRulesetFromCsv(eq("invalidmo_ruleset"), any(MultipartFile.class)))
                .thenThrow(new RuleValidationException("Errors exist in rules.", createRuleValidationErrors()));

        //Get All Rulesets exception
        when(ruleSetService.getAllRulesetMetadata()).thenThrow(new RuntimeException());

        //Get Id 99ea562f-87cc-424d-9835-f569b15b8eef IOException
        when(ruleSetService.existsById(uuid)).thenReturn(true);
        when(ruleService.getRulesForRulesetId(uuid)).thenReturn(rules);
        when(ruleSetService.createCsvFromRuleset(uuid)).thenThrow(new IOException());
        //Get Id 4b40f3b8-0c51-4d7e-90e6-00c366238f05 NotFound
        when(ruleService.getRulesForRulesetId(UUID.fromString("4b40f3b8-0c51-4d7e-90e6-00c366238f05"))).thenReturn(emptyRulesList);

        //Delete ruleset when ruleset does not exist
        when(ruleSetService.getRulesetNameFromId(UUID.fromString("4b40f3b8-0c51-4d7e-90e6-00c366238f04"))).thenThrow(new IllegalArgumentException());

        //Delete ruleset when it's in use by a job config
        final String rulsetInUseName = "ruleset_in_use";
        final UUID uuidInUse = UUID.fromString("8f80f692-0c51-4d7e-5d84-00a644238187");
        when(ruleSetService.getRulesetNameFromId(uuidInUse)).thenReturn(rulsetInUseName);
        when(jobService.isRuleSetInUse(rulsetInUseName)).thenReturn(true);

        //Put ID 99ea562f-87cc-424d-9835-f569b15b8eef CsvRequiredFieldEmptyException
        when(ruleSetService.updateRulesetFromCsv(eq(uuid), any(MultipartFile.class))).thenThrow(new CsvRequiredFieldEmptyException());
        //Put ID 270994ce-249a-4a9b-a87d-2f1c4df0bf94 IOException
        when(ruleSetService.existsById(uuidIoException)).thenReturn(true);

        //Put ID 3f2504e0-4f89-11d3-9a0c-0305e82c3301 RuleValidationException
        when(ruleSetService.updateRulesetFromCsv(eq(DEFAULT_UUID), any(MultipartFile.class))).thenThrow(
                new RuleValidationException("Errors exist in rules.", createRuleValidationErrors()));
        when(ruleSetService.existsById(DEFAULT_UUID)).thenReturn(true);
        when(ruleSetService.getRulesetNameFromId(DEFAULT_UUID)).thenReturn(rulsetInUseName);
        when(ruleSetService.updateRulesetFromCsv(eq(uuidIoException), any(MultipartFile.class))).thenThrow(new IOException());

        RestAssuredMockMvc.webAppContextSetup(context);
    }

    private void mockScopeService() throws CsvRequiredFieldEmptyException, CsvValidationException, IOException {
        when(scopeService.existsByName(eq("duplicate_scope"))).thenReturn(true);

        when(scopeService.existsByName(eq("non_persisted_scope")))
                .thenThrow(new InvalidDataAccessResourceUsageException("could not prepare statement"));

        when(scopeService.createScopeFromCsv(eq("non_existent_scope"), any(MultipartFile.class)))
                .thenThrow(new IOException());
        when(scopeService.createScopeFromCsv(eq("null_value_scope"), any(MultipartFile.class)))
                .thenThrow(new CsvRequiredFieldEmptyException());

        when(maxUploadSizeExceededException.getCause()).thenReturn(illegalStateException);
        when(illegalStateException.getCause()).thenReturn(sizeLimitExceededException);
        when(sizeLimitExceededException.getMessage()).thenReturn(EXCEEDS_THE_CONFIGURED_MAXIMUM);
        when(scopeService.createScopeFromCsv(eq("file_too_large_scope"), any(MultipartFile.class)))
                .thenThrow(maxUploadSizeExceededException);

        final UUID inUseUUID = UUID.fromString("6c9863db-52ff-45e7-97d3-82fff93ba639");
        final Scope scope = new Scope();
        scope.setId(inUseUUID);
        scope.setName("inUseScopeName");
        when(scopeService.getScopeFromUUID(inUseUUID)).thenReturn(scope);
        when(jobService.isScopeInUse(eq("inUseScopeName"))).thenReturn(true);
        when(executionService.isScopeInUse(eq("inUseScopeName"))).thenReturn(true);

        final UUID notExistingUUID = UUID.fromString("d813c56e-209d-11ee-be56-0242ac120002");
        when(scopeService.getScopeFromUUID(eq(notExistingUUID))).thenThrow(IllegalArgumentException.class);
        when(scopeService.getAllScopeMetadata()).thenThrow(RuntimeException.class);
        when(scopeService.getCsvForScope(eq(UUID.fromString("70352292-24af-11ee-be56-0242ac120002")))).thenThrow(IOException.class);
        when(scopeService.getCsvForScope(eq(UUID.fromString("d553052c-24af-11ee-be56-0242ac120002")))).thenThrow(IllegalArgumentException.class);

        final UUID invalidParsingUUID = UUID.fromString("70352292-24af-11ee-be56-0242ac120002");
        final Scope scope_2 = new Scope();
        scope_2.setId(invalidParsingUUID);
        scope_2.setName("invalidParsingScope");
        when(scopeService.getScopeFromUUID(eq(invalidParsingUUID))).thenReturn(scope_2);
        when(jobService.isScopeInUse(eq("invalidParsingScope"))).thenReturn(false);
        when(scopeService.updateScope(eq(scope_2), any(MultipartFile.class))).thenThrow(IOException.class);

        final UUID tooLargeUUID = UUID.fromString("e326e766-2c63-11ee-be56-0242ac120002");
        final Scope scope_3 = new Scope();
        scope_3.setId(tooLargeUUID);
        scope_3.setName("tooLargeScope");
        when(scopeService.getScopeFromUUID(eq(tooLargeUUID))).thenReturn(scope_3);
        when(jobService.isScopeInUse(eq("tooLargeScope"))).thenReturn(false);
        when(scopeService.updateScope(eq(scope_3), any(MultipartFile.class))).thenThrow(maxUploadSizeExceededException);
    }

    private void mockRevertTestCases() throws ChangesInProgressException, UnsupportedOperationException,ProposedIdsNotFoundException {
        when(executionService.existsByExecutionId(EXECUTION_ID_ONE)).thenReturn(true);
        when(executionService.existsByExecutionId(EXECUTION_ID_TWO)).thenReturn(true);
        when(executionService.existsByExecutionId(EXECUTION_ID_THREE)).thenReturn(true);
        when(executionService.existsByExecutionId(EXECUTION_ID_FOUR)).thenReturn(true);
        doThrow(new ChangesInProgressException(CHANGES_IN_PROGRESS))
                .when(executionService)
                .revertChanges(eq("2"), any(EaccApprovedAuditResults.class));
        doThrow(new ProposedIdsNotFoundException(PROPOSED_IDS_DONT_EXIST))
                .when(executionService)
                .revertChanges(eq("3"), any(EaccApprovedAuditResults.class));
        doThrow(new UnsupportedOperationException(PREVIOUS_JOB_EXECUTION))
                .when(executionService)
                .revertChanges(eq("4"), any(EaccApprovedAuditResults.class));
    }

    private void mockApplyTestCases() throws ChangesInProgressException, InconsistencyProcessingFailedException, ProposedIdsNotFoundException {
        doThrow(new ProposedIdsNotFoundException(PROPOSED_IDS_DONT_EXIST))
                .when(executionService)
                .processApprovedChanges(eq("3"), any(EaccApprovedAuditResults.class));
        doThrow(new ChangesInProgressException(CHANGES_IN_PROGRESS))
                .when(executionService)
                .processApprovedChanges(eq("2"), any(EaccApprovedAuditResults.class));
    }

    private void mockAuditResultTestCases() {
        when(executionService.existsByExecutionId(EXECUTION_ID_NOT_EXISTING)).thenReturn(false);
    }

    List<RuleValidationError> createRuleValidationErrors() {
        return List.of(
                new RuleValidationError(
                        2L, "Invalid MO.",
                        "MO not found in Managed Object Model.", ""));
    }
}