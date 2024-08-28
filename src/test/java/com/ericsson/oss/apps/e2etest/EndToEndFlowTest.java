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

package com.ericsson.oss.apps.e2etest;

import static com.ericsson.oss.apps.executor.ExecutionStatus.REVERSION_PARTIALLY_SUCCESSFUL;
import static com.ericsson.oss.apps.util.Constants.BLANK_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.BLANK_RULESET_ERROR;
import static com.ericsson.oss.apps.util.Constants.BLANK_SCHEDULE_ERROR;
import static com.ericsson.oss.apps.util.Constants.DN_PREFIX;
import static com.ericsson.oss.apps.util.Constants.EMPTY_STRING;
import static com.ericsson.oss.apps.util.Constants.EXISTING_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.INVALID_CRON_SCHEDULE_ERROR;
import static com.ericsson.oss.apps.util.Constants.JOBNAME_LENGTH_ERROR;
import static com.ericsson.oss.apps.util.Constants.NONEXISTENT_RULESET_ERROR;
import static com.ericsson.oss.apps.util.Constants.NONEXISTENT_SCOPE_ERROR;
import static com.ericsson.oss.apps.util.Constants.REGEX_JOBNAME_ERROR;
import static com.ericsson.oss.apps.util.Constants.RULESETS_URI;
import static com.ericsson.oss.apps.util.Constants.SCOPES_URI;
import static com.ericsson.oss.apps.util.Constants.VALIDATION_FAILED;
import static com.ericsson.oss.apps.util.Constants.VERSION_PATH;
import static com.ericsson.oss.apps.util.DateUtil.getCurrentUtcDateTime;
import static com.ericsson.oss.apps.util.MetricConstants.APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.APIGATEWAY_SESSIONID_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_APPLIED_CHANGES_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_APPLIED_REVERSIONS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_CHANGES_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_CI_REVERSIONS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_ATTR_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_MO_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_RULES_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_EXECUTIONS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_HTTP_REQUESTS_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.EACC_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_CI_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_CI_HTTP_REQUESTS_TOTAL_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_HTTP_REQUESTS_TOTAL;
import static com.ericsson.oss.apps.util.MetricConstants.NCMP_HTTP_REQUESTS_TOTAL_DURATION_SECONDS;
import static com.ericsson.oss.apps.util.TestDefaults.*;
import static com.ericsson.oss.apps.util.Utilities.createMultipartFile;
import static java.nio.file.StandardOpenOption.READ;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeast;
import static org.mockito.internal.verification.VerificationModeFactory.atMost;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.WebApplicationContext;

import com.ericsson.oss.apps.api.model.EaccJob;
import com.ericsson.oss.apps.client.ncmp.NetworkCmProxyApi;
import com.ericsson.oss.apps.client.ncmp.model.CmHandleQueryParameters;
import com.ericsson.oss.apps.executor.AuditStatus;
import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.executor.ExecutionStatus;
import com.ericsson.oss.apps.executor.ExecutionType;
import com.ericsson.oss.apps.model.AuditResult;
import com.ericsson.oss.apps.model.Execution;
import com.ericsson.oss.apps.model.Job;
import com.ericsson.oss.apps.model.RuleSet;
import com.ericsson.oss.apps.model.Scope;
import com.ericsson.oss.apps.repository.AuditResultsRepository;
import com.ericsson.oss.apps.repository.ExecutionsRepository;
import com.ericsson.oss.apps.repository.JobRepository;
import com.ericsson.oss.apps.repository.RuleSetRepository;
import com.ericsson.oss.apps.repository.ScopeRepository;
import com.ericsson.oss.apps.service.AuditResultService;
import com.ericsson.oss.apps.service.CMServiceException;
import com.ericsson.oss.apps.service.ExecutionService;
import com.ericsson.oss.apps.service.mom.model.Models;
import com.ericsson.oss.apps.service.mom.parser.ModelParser;
import com.ericsson.oss.apps.service.mom.parser.ModelParserException;
import com.ericsson.oss.apps.service.ncmp.NcmpService;
import com.ericsson.oss.apps.service.ncmp.model.CMHandleModuleRevision;
import com.ericsson.oss.apps.util.TestDefaults;
import com.ericsson.oss.apps.util.Utilities;
import com.ericsson.oss.apps.validation.ModelManagerImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * Validates EACC End to End flow, except for CM/NCMP Service. Interaction with CM/NCMP Service is mocked.
 */
@ActiveProfiles("test")
@ExtendWith({ SpringExtension.class, SoftAssertionsExtension.class })
@AutoConfigureObservability
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "modelDiscovery.retry.maxAttempts=3" })
@Slf4j
public class EndToEndFlowTest {

    private static final String V1_JOBS = VERSION_PATH + "/jobs";
    private static final String EXECUTION_ID = "EXECID";
    private static final String EXPECTED_AUDIT_RESULTS_JSON = "json/verification/expectedAuditResults.json";
    private static final String CM_HANDLE = "E323F2F22BB43FA9BA31ABB22C9C00E9";
    private static final String ATTRIBUTES = "attributes";
    private static final String REVERSION_OPTIONS = "fields=ManagedElement/attributes(dnPrefix);" +
            "EUtranCellFDD/attributes(cfraEnable;cellCapMinCellSubCap);";
    private static final String AUDIT_OPTIONS = "fields=ManagedElement/attributes(dnPrefix);" +
            "ENodeBFunction/attributes(forcedSiTunnelingActive;userLabel);"
            + "EUtranCellFDD/attributes(cellCapMinCellSubCap;cellCapMinMaxWriProt;cfraEnable);";
    private static final String JOBNAME_1 = "jobName1";
    private static final String DOLLAR = "$";

    @MockBean
    ApplicationReadyEvent applicationReadyEvent;

    @Autowired
    private final ModelParser modelParser = new ModelParser();

    @Autowired
    private WebApplicationContext webApplicationContext;
    private MockMvc mvc;

    @Value("${ncmpCalls.retry.maxAttempts}")
    private int ncmpCallsMaxRetryAttempts;

    @Value("${modelDiscovery.retry.maxAttempts}")
    private int modelDiscoveryMaxRetryAttempts;

    @SpyBean
    private NcmpService ncmpService;

    @MockBean
    private NetworkCmProxyApi mockedNetworkCmProxyApi;

    @Autowired
    private ExecutionsRepository executionsRepository;

    @Autowired
    private RuleSetRepository ruleSetRepository;

    @Autowired
    private AuditResultsRepository auditResultsRepository;

    @Autowired
    private ScopeRepository scopeRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ModelManagerImpl modelManager;

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private AuditResultService auditResultService;

    @InjectSoftAssertions
    private SoftAssertions softly;

    @BeforeEach
    public void setUp() throws FileNotFoundException, ModelParserException {
        reset(mockedNetworkCmProxyApi);
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        generateModels();
        modelManager.setModelValidationReady(Boolean.TRUE);
    }

    @AfterEach
    void cleanUp() {
        jobRepository.deleteAll();
        ruleSetRepository.deleteAll();
        scopeRepository.deleteAll();
        auditResultsRepository.deleteAll();
        executionsRepository.deleteAll();
        AuditResult.clearIdMap();
    }

    @Test
    public void validate_eacc_e2e_flow() throws Exception {
        ReflectionTestUtils.setField(ncmpService, "networkCmProxyApi", mockedNetworkCmProxyApi);

        // Set up the ruleset & scope files
        final MockMultipartFile rulesetMultipartFile = createMultipartFile(END_TO_END_RULESET_PATH);
        // Two cells are generated below and used for this 1 Node
        final MockMultipartFile scopeMultipartFile = createMultipartFile(END_TO_END_SCOPE_PATH);

        mvc.perform(multipart(RULESETS_URL).file(rulesetMultipartFile).header(AUTHORIZATION, TOKEN).param(RULESET_NAME_PARAM, DEFAULT_RULESET_NAME)
                .header(AUTHORIZATION, TOKEN))
                .andExpect(status().isCreated());
        mvc.perform(multipart(SCOPES_URL).file(scopeMultipartFile).header(AUTHORIZATION, TOKEN).param(SCOPE_NAME_PARAM, DEFAULT_SCOPE_NAME))
                .andExpect(status().isCreated());

        // Set up the job configuration
        //TODO Tests should be time-agnostic, System clock should be injected
        final EaccJob testJob = new EaccJob(DEFAULT_JOB_NAME, DEFAULT_SCOPE_NAME, getSchedule(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(5)),
                DEFAULT_RULESET_NAME);
        when(mockedNetworkCmProxyApi.getResourceDataForCmHandle(any(), eq(CM_HANDLE), any(), eq(AUDIT_OPTIONS), any(), any()))
                .thenReturn(getRawManagedObjectsForAudit());
        when(mockedNetworkCmProxyApi.getResourceDataForCmHandle(any(), eq(CM_HANDLE), any(), eq(REVERSION_OPTIONS), any(), any()))
                .thenReturn(getRawManagedObjectsForReversionValidation());

        mvc.perform(get(JOBS_URL).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(DOLLAR, is(empty())));

        mvc.perform(post(JOBS_URL).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN).content(Utilities.toJson(testJob)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.jobName", is(testJob.getJobName())));

        mvc.perform(get(JOBS_URL).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(DOLLAR, hasSize(1)))
                .andExpect(jsonPath("$[0].jobName", is(testJob.getJobName())));

        await().alias("Waiting for audit to complete.").atMost(20, SECONDS).pollInterval(1, SECONDS)
                .until(isExecutionComplete());

        // Verify the Execution using the specific job name
        mvc.perform(
                get(VERSION_PATH + "/executions?jobName=" + DEFAULT_JOB_NAME).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(DOLLAR, hasSize(1)))
                .andExpect(jsonPath("$[0].jobName", is(testJob.getJobName())))
                .andExpect(jsonPath("$[0].executionStatus", is("Audit Successful")))
                .andExpect(jsonPath("$[0].totalMosAudited", is(3)))
                .andExpect(jsonPath("$[0].totalAttributesAudited", is(6)))
                .andExpect(jsonPath("$[0].inconsistenciesIdentified", is(4)));

        // Verify the Audit Results and ensure Rule with Correct Priority used
        final List<Execution> executionList = executionsRepository.findAll();
        final String executionId = Long.toString(executionList.get(0).getId());
        final String auditResultUrl = VERSION_PATH + "/executions/" + executionId + "/audit-results";
        mvc.perform(get(auditResultUrl).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        Utilities.readFileFromResources(EXPECTED_AUDIT_RESULTS_JSON).replace(EXECUTION_ID, executionId)))
                .andExpect(jsonPath("$.totalElements", is(6)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.currentPage", is(nullValue())))
                .andExpect(jsonPath("$.perPage", is(nullValue())))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.hasPrev", is(false)))
                .andExpect(jsonPath("$.results", hasSize(6)));

        // Verify the Audit Results with Pagination
        mvc.perform(get(auditResultUrl + "?page=0&pageSize=1").contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements", is(6)))
                .andExpect(jsonPath("$.totalPages", is(6)))
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.perPage", is(1)))
                .andExpect(jsonPath("$.hasNext", is(true)))
                .andExpect(jsonPath("$.hasPrev", is(false)))
                .andExpect(jsonPath("$.results", hasSize(1)));

        mvc.perform(get(auditResultUrl + "?page=1&pageSize=1").contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements", is(6)))
                .andExpect(jsonPath("$.totalPages", is(6)))
                .andExpect(jsonPath("$.currentPage", is(1)))
                .andExpect(jsonPath("$.perPage", is(1)))
                .andExpect(jsonPath("$.hasNext", is(true)))
                .andExpect(jsonPath("$.hasPrev", is(true)))
                .andExpect(jsonPath("$.results", hasSize(1)));

        mvc.perform(get(auditResultUrl + "?page=5&pageSize=1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements", is(6)))
                .andExpect(jsonPath("$.totalPages", is(6)))
                .andExpect(jsonPath("$.currentPage", is(5)))
                .andExpect(jsonPath("$.perPage", is(1)))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.hasPrev", is(true)))
                .andExpect(jsonPath("$.results", hasSize(1)));

        // Verify the Audit Results with Filter
        mvc.perform(get(auditResultUrl + "?page=0&pageSize=6&filter=auditStatus:Inconsistent").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json(Utilities.readFileFromResources("json/verification" +
                        "/expectedAuditResultsFilteredByInconsistent" +
                        ".json").replace(EXECUTION_ID, executionId)))
                .andExpect(jsonPath("$.totalElements", is(4)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.perPage", is(6)))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.hasPrev", is(false)))
                .andExpect(jsonPath("$.results", hasSize(4)));

        mvc.perform(get(auditResultUrl + "?page=0&pageSize=6&filter=auditStatus:(Inconsistent," +
                "Consistent)").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content()
                        .json(Utilities.readFileFromResources("json/verification/expectedAuditResultsFilteredByInconsistentAndConsistent.json")
                                .replace(EXECUTION_ID, executionId)))
                .andExpect(jsonPath("$.totalElements", is(6)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.perPage", is(6)))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.hasPrev", is(false)))
                .andExpect(jsonPath("$.results", hasSize(6)));

        mvc.perform(get(auditResultUrl
                + "?filter=managedObjectFdn:%SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW,ManagedElement=LTE416dg2ERBS00006,ENodeBFunction=1,EUtranCellFDD=LTE06dg2ERBS00030-3%")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content()
                        .json(Utilities.readFileFromResources("json/verification/expectedAuditResultsFilteredByMo.json")
                                .replace(EXECUTION_ID, executionId)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.currentPage", is(nullValue())))
                .andExpect(jsonPath("$.perPage", is(nullValue())))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.hasPrev", is(false)))
                .andExpect(jsonPath("$.results", hasSize(3)));

        // Verify Apply Change
        Mockito.doNothing().when(ncmpService).patchManagedObject(any());
        final String applyChangeJsonString = """
                {
                "auditResultIds": ["3","4"]
                }
                """;

        mvc.perform(post(auditResultUrl).contentType(MediaType.APPLICATION_JSON).content(applyChangeJsonString).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isAccepted());
        mvc.perform(get(auditResultUrl).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements", is(6)));

        await().alias("Waiting for changes to complete.").atMost(20, SECONDS).pollInterval(1, SECONDS).until(changesApplied());

        mvc.perform(
                get(VERSION_PATH + "/executions?jobName=" + DEFAULT_JOB_NAME).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(DOLLAR, hasSize(1)))
                .andExpect(jsonPath("$[0].executionStatus", is("Changes Successful")));

        // Verify Revert Change
        final String revertChangeJsonString = """
                {
                "auditResultIds": ["3","4"],
                "operation": "REVERT_CHANGE"
                }
                """;
        mvc.perform(post(auditResultUrl).contentType(MediaType.APPLICATION_JSON).content(revertChangeJsonString))
                .andExpect(status().isAccepted());
        mvc.perform(get(auditResultUrl).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalElements", is(6)));

        await().alias("Waiting for reversions to complete.").atMost(20, SECONDS).pollInterval(1, SECONDS).until(reversionApplied());

        mvc.perform(get(VERSION_PATH + "/executions?jobName=" + DEFAULT_JOB_NAME).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(DOLLAR, hasSize(1)))
                .andExpect(jsonPath("$[0].executionStatus", is("Reversion Partially Successful")));

        final var changes = auditResultsRepository.findAll().stream()
                .filter(auditResult -> (auditResult.getChangeStatus().equals(ChangeStatus.REVERSION_COMPLETE) ||
                        auditResult.getChangeStatus().equals(ChangeStatus.REVERSION_FAILED)))
                .toList();

        assertThat(changes)
                .hasSize(2)
                .extracting(AuditResult::getId, AuditResult::getChangeStatus)
                .containsExactlyInAnyOrder(tuple(3L, ChangeStatus.REVERSION_FAILED),
                        tuple(4L, ChangeStatus.REVERSION_COMPLETE));

        // Verify the Metrics
        final String metricsOutput = mvc.perform(get("/actuator/prometheus").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        verifyMetrics(metricsOutput, EACC_HTTP_REQUESTS_TOTAL, EACC_HTTP_REQUESTS_DURATION_SECONDS,
                APIGATEWAY_SESSIONID_HTTP_REQUESTS_TOTAL, APIGATEWAY_SESSIONID_HTTP_REQUESTS_DURATION_SECONDS,
                NCMP_HTTP_REQUESTS_TOTAL, NCMP_HTTP_REQUESTS_TOTAL_DURATION_SECONDS,
                EACC_EXECUTIONS_TOTAL, EACC_EXECUTIONS_MO_TOTAL,
                EACC_EXECUTIONS_RULES_TOTAL, EACC_EXECUTIONS_ATTR_TOTAL,
                EACC_EXECUTIONS_DURATION_SECONDS, EACC_CI_APPLIED_CHANGES_TOTAL, EACC_CI_HTTP_REQUESTS_TOTAL,
                EACC_CI_CHANGES_DURATION_SECONDS, NCMP_CI_HTTP_REQUESTS_TOTAL_DURATION_SECONDS, NCMP_CI_HTTP_REQUESTS_TOTAL,
                EACC_CI_HTTP_REQUESTS_DURATION_SECONDS, EACC_CI_APPLIED_REVERSIONS_TOTAL, EACC_CI_REVERSIONS_DURATION_SECONDS);

        // Verify Delete Job
        mvc.perform(delete(V1_JOBS + SLASH + DEFAULT_JOB_NAME).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isNoContent());
        // Verify the Job doesn't exist after deletion
        mvc.perform(get(V1_JOBS).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(DOLLAR, is(empty())));

        // Verify patch called 3 times - two for consistency, one for reversion
        verify(mockedNetworkCmProxyApi, times(3)).patchResourceDataRunningForCmHandle(any(), any(), any(), any(), any());
    }

    @Test
    public void whenRulesetIsCreatedWithMissingValues_thenBadRequestResponseIsReceived() throws Exception {
        final MockMultipartFile rulesetMultipartFile = createMultipartFile(INCORRECT_COLUMNS_RULESET_PATH);

        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(multipart(RULESETS_URL).file(rulesetMultipartFile).param(RULESET_NAME_PARAM, DEFAULT_RULESET_NAME).header(AUTHORIZATION,
                        TOKEN)),
                "File cannot contain missing values and column headers must match the Rule model.", is(FILL_IN_ALL_REQUIRED_FIELDS));
    }

    @Test
    public void whenRulesetIsCreatedWithIncorrectColumns_thenBadRequestResponseIsReceived() throws Exception {
        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(multipart(RULESETS_URL).file(createMultipartFile(INCORRECT_COLUMNS_RULESET_PATH)).header(AUTHORIZATION, TOKEN)
                        .param(RULESET_NAME_PARAM, DEFAULT_RULESET_NAME)),
                "File cannot contain missing values and column headers must match the Rule model.", is(FILL_IN_ALL_REQUIRED_FIELDS));
    }

    @Test
    public void whenRulesetIsCreatedWithInvalidMo_thenBadRequestResponseIsReceived() throws Exception {
        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(multipart(RULESETS_URL).file(createMultipartFile(INVALID_MO_RULESET_PATH)).param(RULESET_NAME_PARAM,
                        DEFAULT_RULESET_NAME).header(AUTHORIZATION, TOKEN)),
                "Problems found in ruleset.",
                is("Ruleset cannot contain any invalid MO types, attributes or values."),
                jsonPath(JSON_RULE_VALIDATION_ERRORS, hasSize(1)),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].lineNumber", is(2)),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].errorType", is("Invalid MO.")),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].errorDetails", is("MO not found in Managed Object Model.")),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].additionalInfo", is("")));
    }

    @Test
    public void whenRulesetIsCreatedWithInvalidAttribute_thenBadRequestResponseIsReceived() throws Exception {
        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(multipart(RULESETS_URL).file(createMultipartFile(INVALID_ATTRIBUTE_RULESET_PATH))
                        .param(RULESET_NAME_PARAM, DEFAULT_RULESET_NAME).header(AUTHORIZATION, TOKEN)),
                "Problems found in ruleset.",
                is("Ruleset cannot contain any invalid MO types, attributes or values."),
                jsonPath(JSON_RULE_VALIDATION_ERRORS, hasSize(1)),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].lineNumber", is(3)),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].errorType", is("Invalid Attribute name.")),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].errorDetails", is("Attribute not found in Managed Object Model.")),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].additionalInfo", is("")));
    }

    @Test
    public void whenRulesetIsCreatedWithInvalidAttributeValue_thenBadRequestResponseIsReceived() throws Exception {
        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(multipart(RULESETS_URL).file(createMultipartFile(INVALID_ATTRIBUTE_VALUE_RULESET_PATH))
                        .param(RULESET_NAME_PARAM, DEFAULT_RULESET_NAME).header(AUTHORIZATION, TOKEN)),
                "Problems found in ruleset.",
                is("Ruleset cannot contain any invalid MO types, attributes or values."),
                jsonPath(JSON_RULE_VALIDATION_ERRORS, hasSize(1)),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].lineNumber", is(4)),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].errorType", is("Invalid Attribute value.")),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].errorDetails",
                        is("Attribute value is invalid according to the Managed Object Model.")),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].additionalInfo", is("")));
    }

    @Test
    public void whenRulesetIsCreatedWithInvalidCondition_thenBadRequestResponseIsReceived() throws Exception {
        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(multipart(RULESETS_URL).file(createMultipartFile(INVALID_CONDITION_RULESET_PATH))
                        .param(RULESET_NAME_PARAM, DEFAULT_RULESET_NAME).header(AUTHORIZATION, TOKEN)),
                "Problems found in ruleset.",
                is("Ruleset cannot contain any invalid MO types, attributes or values."),
                jsonPath(JSON_RULE_VALIDATION_ERRORS, hasSize(1)),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].lineNumber", is(3)),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].errorType", is("Invalid Condition.")),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].errorDetails",
                        is("Condition has invalid syntax.")),
                jsonPath(JSON_RULE_VALIDATION_ERRORS + "[0].additionalInfo", is("")));
    }

    @Test
    public void whenRulesetIsUpdatedCorrectly_thenSuccessfulResponseIsReceived() throws Exception {
        final MockMultipartFile rulesetMultipartFile = createMultipartFile(END_TO_END_RULESET_PATH);
        mvc.perform(multipart(RULESETS_URL).file(rulesetMultipartFile).param(RULESET_NAME_PARAM, DEFAULT_RULESET_NAME).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isCreated());

        final Optional<RuleSet> ruleSet = ruleSetRepository.findByName(DEFAULT_RULESET_NAME);
        final String ruleSetId = ruleSet.map(RuleSet::getId)
                .map(UUID::toString).orElseThrow(IllegalStateException::new);
        final MockMultipartFile updatedRulesetMultipartFile = createMultipartFile(END_TO_END_UPDATED_RULESET_PATH);

        final MockMultipartHttpServletRequestBuilder requestBuilder = multipart(RULESETS_URL + SLASH + ruleSetId).file(updatedRulesetMultipartFile);
        requestBuilder.with(request -> {
            request.setMethod("PUT");
            request.addHeader(AUTHORIZATION, TOKEN);
            return request;
        });

        mvc.perform(get(RULESETS_URL + SLASH + ruleSetId).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(content().string(Utilities.readFileFromResources("expectedRuleset.csv")));

        mvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_ID, is(ruleSetId)))
                .andExpect(jsonPath(JSON_RULESET_NAME, is(DEFAULT_RULESET_NAME)))
                .andExpect(jsonPath(JSON_URI, is(RULESETS_URI + ruleSetId)));

        mvc.perform(get(RULESETS_URL + SLASH + ruleSetId).header(AUTHORIZATION, TOKEN))
                .andExpect(content().json(Utilities.readFileFromResources("expectedUpdatedRuleset.csv")));
    }

    @Test
    public void whenRulesetIsUpdatedWhileInUseByAJob_thenConflictResponseIsReceived() throws Exception {
        final MockMultipartFile rulesetMultipartFile = createMultipartFile(END_TO_END_RULESET_PATH);
        final MockMultipartFile updatedRulesetMultipartFile = createMultipartFile(END_TO_END_UPDATED_RULESET_PATH);

        final Execution execution = new Execution();
        execution.setExecutionStatus(ExecutionStatus.AUDIT_IN_PROGRESS);
        execution.setRuleSetName(DEFAULT_RULESET_NAME);
        executionsRepository.save(execution);

        mvc.perform(multipart(RULESETS_URL).file(rulesetMultipartFile).header(AUTHORIZATION, TOKEN).param(RULESET_NAME_PARAM, DEFAULT_RULESET_NAME))
                .andExpect(status().isCreated());

        final Optional<RuleSet> ruleSet = ruleSetRepository.findByName(DEFAULT_RULESET_NAME);
        final String ruleSetId = ruleSet.map(RuleSet::getId)
                .map(UUID::toString).orElseThrow(IllegalStateException::new);
        final var requestBuilder = multipart(RULESETS_URL + SLASH + ruleSetId).file(updatedRulesetMultipartFile).header(AUTHORIZATION, TOKEN);
        requestBuilder.with(request -> {
            request.setMethod("PUT");
            return request;
        });

        mvc.perform(requestBuilder)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(JSON_TITLE, is("Cannot update ruleset as it is used by an ongoing execution.")))
                .andExpect(jsonPath(JSON_DETAIL, is("Ruleset in use.")));
    }

    @Test
    public void validate_eacc_exceptionResponse_JobController() throws Exception {
        final Job job = new Job();
        //Verify that exception is thrown for bad Job request
        mvc.perform(post(V1_JOBS).content(Utilities.toJson(job)).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void validate_post_job() throws Exception {
        // Validation flow:
        // Already existing Job
        // Already existing Ruleset
        // Already existing Scope
        // Other annotations on the Job object

        // Ruleset File Setup for Validation
        final InputStream fileInputStream = Files.newInputStream(Path.of(END_TO_END_RULESET_PATH), READ);

        final MockMultipartFile fileName = new MockMultipartFile(DEFAULT_FILE_NAME, fileInputStream);

        final InputStream fileInputStream1 = Files.newInputStream(Path.of(END_TO_END_SCOPE_PATH), READ);

        final MockMultipartFile fileName1 = new MockMultipartFile(DEFAULT_FILE_NAME, fileInputStream1);

        mvc.perform(multipart(SCOPES_URL).file(fileName1).param("scopeName", SCOPE_ATHLONE).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isCreated());

        final EaccJob validTestJob = new EaccJob(VALID_JOBNAME_FOR_VALIDATION, SCOPE_ATHLONE, VALID_SCHEDULE_FOR_VALIDATION, DEFAULT_RULESET_NAME);
        final EaccJob testJobInvalidRegexName = new EaccJob("invalid%", SCOPE_ATHLONE, VALID_SCHEDULE_FOR_VALIDATION, DEFAULT_RULESET_NAME);
        final EaccJob testJobInvalidShortName = new EaccJob("tes", SCOPE_ATHLONE, VALID_SCHEDULE_FOR_VALIDATION, DEFAULT_RULESET_NAME);
        final EaccJob testJobInvalidBlankName = new EaccJob(EMPTY_STRING, SCOPE_ATHLONE, VALID_SCHEDULE_FOR_VALIDATION, DEFAULT_RULESET_NAME);
        final EaccJob testJobInvalidWhitespaceName = new EaccJob("    ", SCOPE_ATHLONE, VALID_SCHEDULE_FOR_VALIDATION, DEFAULT_RULESET_NAME);
        final EaccJob testJobInvalidNullName = new EaccJob(null, SCOPE_ATHLONE, VALID_SCHEDULE_FOR_VALIDATION, DEFAULT_RULESET_NAME);
        final EaccJob testJobInvalidCron = new EaccJob(VALID_JOBNAME_FOR_VALIDATION, SCOPE_ATHLONE, "0 15 11 ? & *", DEFAULT_RULESET_NAME);
        final EaccJob testJobBlankCron = new EaccJob(VALID_JOBNAME_FOR_VALIDATION, SCOPE_ATHLONE, EMPTY_STRING, DEFAULT_RULESET_NAME);
        final EaccJob testJobNullCron = new EaccJob(VALID_JOBNAME_FOR_VALIDATION, SCOPE_ATHLONE, null, DEFAULT_RULESET_NAME);
        final EaccJob testJobUnknownRulesetName = new EaccJob(VALID_JOBNAME_FOR_VALIDATION, SCOPE_ATHLONE, VALID_SCHEDULE_FOR_VALIDATION, "test");
        final EaccJob testJobBlankRulesetName = new EaccJob(VALID_JOBNAME_FOR_VALIDATION, SCOPE_ATHLONE, VALID_SCHEDULE_FOR_VALIDATION, EMPTY_STRING);
        final EaccJob testJobNullRulesetName = new EaccJob(VALID_JOBNAME_FOR_VALIDATION, SCOPE_ATHLONE, VALID_SCHEDULE_FOR_VALIDATION, null);
        final EaccJob testJobNullScopeName = new EaccJob(VALID_JOBNAME_FOR_VALIDATION, null, VALID_SCHEDULE_FOR_VALIDATION, DEFAULT_RULESET_NAME);
        final EaccJob testJobNonUnknownScopeName = new EaccJob(VALID_JOBNAME_FOR_VALIDATION, "unknown", VALID_SCHEDULE_FOR_VALIDATION,
                DEFAULT_RULESET_NAME);
        final EaccJob testJobBlankScopeName = new EaccJob(VALID_JOBNAME_FOR_VALIDATION, EMPTY_STRING, VALID_SCHEDULE_FOR_VALIDATION,
                DEFAULT_RULESET_NAME);
        final EaccJob testNullScheduleWithValidJobNameRulesetName = new EaccJob(VALID_JOBNAME_FOR_VALIDATION, SCOPE_ATHLONE, null,
                DEFAULT_RULESET_NAME);
        final EaccJob testNullJobNameAndScheduleWithValidRulesetName = new EaccJob(null, SCOPE_ATHLONE, null, DEFAULT_RULESET_NAME);

        // Verify Valid job name returns a Bad Request response because of no ruleset found
        mvc.perform(post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(validTestJob)).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(JSON_TITLE, is(VALIDATION_FAILED)))
                .andExpect(jsonPath(JSON_STATUS, is(BAD_REQUEST)))
                .andExpect(jsonPath(JSON_DETAIL,
                        is(NONEXISTENT_RULESET_ERROR)));

        // Post valid ruleset
        modelManager.setModelValidationReady(Boolean.TRUE);
        mvc.perform(multipart(RULESETS_URL).file(fileName).param("rulesetName", DEFAULT_RULESET_NAME).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isCreated());

        // Verify jobName
        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobInvalidShortName))
                        .header(AUTHORIZATION, TOKEN)),
                VALIDATION_FAILED, containsString(JOBNAME_LENGTH_ERROR));

        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobInvalidRegexName))
                        .header(AUTHORIZATION, TOKEN)),
                VALIDATION_FAILED, containsString(REGEX_JOBNAME_ERROR));

        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobInvalidBlankName))
                        .header(AUTHORIZATION, TOKEN)),
                VALIDATION_FAILED, containsString(BLANK_JOBNAME_ERROR));

        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobInvalidWhitespaceName))
                        .header(AUTHORIZATION, TOKEN)),
                VALIDATION_FAILED, containsString(BLANK_JOBNAME_ERROR));

        mvc.perform(
                post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobInvalidNullName)).header(AUTHORIZATION,
                        TOKEN))
                .andExpect(status().isBadRequest());

        // Verify Schedule
        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(
                        post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobInvalidCron)).header(AUTHORIZATION,
                                TOKEN)),
                VALIDATION_FAILED,
                containsString(INVALID_CRON_SCHEDULE_ERROR));

        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobBlankCron)).header(AUTHORIZATION,
                        TOKEN)),
                VALIDATION_FAILED,
                allOf(containsString(BLANK_SCHEDULE_ERROR), containsString(INVALID_CRON_SCHEDULE_ERROR)));

        mvc.perform(post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobNullCron)).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isBadRequest());

        // Verify ruleset
        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobUnknownRulesetName))
                        .header(AUTHORIZATION, TOKEN)),
                VALIDATION_FAILED, containsString(NONEXISTENT_RULESET_ERROR));

        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobBlankRulesetName))
                        .header(AUTHORIZATION, TOKEN)),
                VALIDATION_FAILED, containsString(BLANK_RULESET_ERROR));

        // Null rulesetName
        mvc.perform(
                post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobNullRulesetName)).header(AUTHORIZATION,
                        TOKEN))
                .andExpect(status().isBadRequest());

        // Null scopeName
        mvc.perform(
                post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobNullScopeName)).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isBadRequest());

        // Blank scopeName
        mvc.perform(
                post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobBlankScopeName)).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(JSON_STATUS, is(BAD_REQUEST)))
                .andExpect(jsonPath(JSON_DETAIL, containsString(NONEXISTENT_SCOPE_ERROR)));

        // Unknown scopeName
        mvc.perform(
                post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testJobNonUnknownScopeName)).header(AUTHORIZATION,
                        TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(JSON_STATUS, is(BAD_REQUEST)))
                .andExpect(jsonPath(JSON_DETAIL, containsString(NONEXISTENT_SCOPE_ERROR)));

        // Null jobName and schedule with valid Ruleset
        mvc.perform(post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testNullJobNameAndScheduleWithValidRulesetName))
                .header(AUTHORIZATION, TOKEN))
                .andExpect(status().isBadRequest());

        // Valid jobName and null schedule with valid Ruleset
        mvc.perform(post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(testNullScheduleWithValidJobNameRulesetName))
                .header(AUTHORIZATION, TOKEN))
                .andExpect(status().isBadRequest());

        // Verify valid job is added to the database
        mvc.perform(post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(validTestJob)).header(AUTHORIZATION, TOKEN));
        mvc.perform(get(V1_JOBS).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(DOLLAR, hasSize(1)))
                .andExpect(jsonPath("$[0].jobName", is(validTestJob.getJobName())));

        // Verify duplicate job name returns a Bad Request response
        assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(
                mvc.perform(
                        post(V1_JOBS).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(validTestJob)).header(AUTHORIZATION, TOKEN)),
                VALIDATION_FAILED,
                containsString(EXISTING_JOBNAME_ERROR));
    }

    @Test
    public void whenScopeIsUpdatedCorrectly_thenSuccessfulResponseIsReceived() throws Exception {
        final MockMultipartFile scopeMultipartFile = createMultipartFile(END_TO_END_SCOPE_PATH);
        final MockMultipartFile updatedScopeMultipartFile = createMultipartFile(END_TO_END_UPDATED_SCOPE_PATH);

        mvc.perform(multipart(SCOPES_URL).file(scopeMultipartFile).header(AUTHORIZATION, TOKEN).param(SCOPE_NAME_PARAM, DEFAULT_SCOPE_NAME))
                .andExpect(status().isCreated());

        final Optional<Scope> scopeAthlone = scopeRepository.findByName(DEFAULT_SCOPE_NAME);
        final String scopeId = scopeAthlone.map(Scope::getId)
                .map(UUID::toString).orElseThrow(IllegalStateException::new);

        final var requestBuilder = multipart(SCOPES_URL + SLASH + scopeId).file(updatedScopeMultipartFile).header(AUTHORIZATION, TOKEN);
        requestBuilder.with(request -> {
            request.setMethod("PUT");
            return request;
        });

        mvc.perform(get(SCOPES_URL + SLASH + scopeId).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(content().string(Utilities.readFileFromResources("expectedScope.csv")));

        mvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_ID, is(scopeId)))
                .andExpect(jsonPath(JSON_SCOPE_NAME, is(DEFAULT_SCOPE_NAME)))
                .andExpect(jsonPath(JSON_URI, is(SCOPES_URI + scopeId)));

        mvc.perform(get(SCOPES_URL + SLASH + scopeId).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(content().string(Utilities.readFileFromResources("expectedUpdatedScope.csv")));
    }

    @Test
    public void whenJobIsUpdatedCorrectly_thenSuccessfulResponseIsReceived() throws Exception {
        final MockMultipartFile rulesetMultipartFile = createMultipartFile(END_TO_END_RULESET_PATH);
        final MockMultipartFile scopeMultipartFile = createMultipartFile(END_TO_END_SCOPE_PATH);

        mvc.perform(
                multipart(RULESETS_URL).file(rulesetMultipartFile).header(AUTHORIZATION, TOKEN).param(RULESET_NAME_PARAM, DEFAULT_RULESET_NAME + "1"))
                .andExpect(status().isCreated());
        mvc.perform(
                multipart(RULESETS_URL).file(rulesetMultipartFile).header(AUTHORIZATION, TOKEN).param(RULESET_NAME_PARAM, DEFAULT_RULESET_NAME + "2"))
                .andExpect(status().isCreated());
        mvc.perform(multipart(SCOPES_URL).file(scopeMultipartFile).param(SCOPE_NAME_PARAM, DEFAULT_SCOPE_NAME + "1").header(AUTHORIZATION, TOKEN))
                .andExpect(status().isCreated());
        mvc.perform(multipart(SCOPES_URL).file(scopeMultipartFile).param(SCOPE_NAME_PARAM, DEFAULT_SCOPE_NAME + "2").header(AUTHORIZATION, TOKEN))
                .andExpect(status().isCreated());

        final EaccJob job = new EaccJob(VALID_JOBNAME_FOR_VALIDATION, DEFAULT_SCOPE_NAME + "1", VALID_SCHEDULE_FOR_VALIDATION,
                DEFAULT_RULESET_NAME + "1");

        mvc.perform(post(JOBS_URL).contentType(MediaType.APPLICATION_JSON).content(Utilities.toJson(job)).header(AUTHORIZATION, TOKEN))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_JOB_NAME, is(job.getJobName())))
                .andExpect(jsonPath(JSON_SCOPE_NAME, is(job.getScopeName())))
                .andExpect(jsonPath(JSON_SCHEDULE, is(job.getSchedule())))
                .andExpect(jsonPath(JSON_RULESET_NAME, is(job.getRulesetName())));

        mvc.perform(get(JOBS_URL).header(AUTHORIZATION, TOKEN).contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(DOLLAR, hasSize(1)))
                .andExpect(jsonPath("$[0].scopeName", is(DEFAULT_SCOPE_NAME + "1")))
                .andExpect(jsonPath("$[0].rulesetName", is(DEFAULT_RULESET_NAME + "1")));

        job.setRulesetName(DEFAULT_RULESET_NAME + "2");
        job.setScopeName(DEFAULT_SCOPE_NAME + "2");

        mvc.perform(put(JOBS_URL + SLASH + VALID_JOBNAME_FOR_VALIDATION).contentType(MediaType.APPLICATION_JSON)
                .content(Utilities.toJson(job)).header(AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(JSON_JOB_NAME, is(VALID_JOBNAME_FOR_VALIDATION)))
                .andExpect(jsonPath(JSON_RULESET_NAME, is(DEFAULT_RULESET_NAME + "2")))
                .andExpect(jsonPath(JSON_SCOPE_NAME, is(DEFAULT_SCOPE_NAME + "2")))
                .andExpect(jsonPath(JSON_SCHEDULE, is(VALID_SCHEDULE_FOR_VALIDATION)));

        mvc.perform(get(JOBS_URL).contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(DOLLAR, hasSize(1)))
                .andExpect(jsonPath("$[0].scopeName", is(DEFAULT_SCOPE_NAME + "2")))
                .andExpect(jsonPath("$[0].rulesetName", is(DEFAULT_RULESET_NAME + "2")));
    }

    @Test
    public void validate_apply_changes_for_single_change() throws Exception {
        final Execution createdExecution = executionsRepository.save(createDefaultExecution());
        final String execId = createdExecution.getId().toString();
        auditResultsRepository.saveAll(createAuditResults(execId).subList(0, 1));

        final Optional<List<AuditResult>> auditResultsOptionalForExecId = auditResultsRepository.findByExecutionId(execId);
        assertThat(auditResultsOptionalForExecId).as(TestDefaults.TEST_SETUP_ERROR).isPresent();
        final List<AuditResult> auditResultsForExecId = auditResultsOptionalForExecId.get();
        assertThat(auditResultsOptionalForExecId).as(TestDefaults.TEST_SETUP_ERROR).isNotEmpty();
        assertThat(auditResultsForExecId.get(0)).as(TestDefaults.TEST_SETUP_ERROR).isNotIn(APPLIED_OR_IN_PROGRESS);
        final String approvedAuditResults = """
                {
                    "auditResultIds": ["1"]
                }""";

        mvc.perform(post(VERSION_PATH + "/executions/" + execId + "/audit-results")
                .contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN)
                .content(approvedAuditResults))
                .andExpect(status().isAccepted());

        final Optional<List<AuditResult>> auditResultsAfterImplementationOptionalForExecId = auditResultsRepository.findByExecutionId(execId);
        assertThat(auditResultsAfterImplementationOptionalForExecId).isPresent();
        final List<AuditResult> auditResultsAfterImplementationForExecId = auditResultsAfterImplementationOptionalForExecId.get();
        final AuditResult auditResultAfterImplementation = auditResultsAfterImplementationForExecId.get(0);
        assertThat(auditResultAfterImplementation).isNotNull();

        await().alias("Waiting for change implementation to complete.").atMost(10, SECONDS).with().pollInterval(1, SECONDS)
                .until(() -> APPLIED_OR_IN_PROGRESS.contains(auditResultAfterImplementation.getChangeStatus()));

        auditResultsRepository.deleteByExecutionIdAndId(execId, 1L);
        assertThat(auditResultsRepository.findAll()).isEmpty();
    }

    @Test
    public void validate_apply_changes_for_multiple_changes() throws Exception {
        final Execution createdExecution = executionsRepository.save(createDefaultExecution());
        final String execId = createdExecution.getId().toString();
        auditResultsRepository.saveAll(createAuditResults(execId));
        final var auditResults = auditResultsRepository.findByExecutionId(execId).orElseThrow();
        for (final AuditResult result : auditResults) {
            assertThat(result.getChangeStatus()).as(TestDefaults.TEST_SETUP_ERROR)
                    .isNotIn(APPLIED_OR_IN_PROGRESS);
        }
        final List<Long> ids = List.of(1L, 2L);
        final String approvedAuditResults = """
                {
                    "auditResultIds": ["%d", "%d"]
                }""".formatted(ids.get(0), ids.get(1));

        mvc.perform(post(VERSION_PATH + "/executions/" + execId + "/audit-results")
                .contentType(MediaType.APPLICATION_JSON).header(AUTHORIZATION, TOKEN)
                .content(approvedAuditResults))
                .andExpect(status().isAccepted());

        final List<AuditResult> finalAuditResults = auditResultsRepository.findByExecutionId(execId).orElseThrow();
        assertThat(finalAuditResults).hasSize(2);
        for (int i = 0; i < finalAuditResults.size(); i++) {
            final ChangeStatus actualChangeStatus = finalAuditResults.get(i).getChangeStatus();

            await().alias("Waiting for change implementation to complete.").atMost(20, SECONDS).pollInterval(1, SECONDS)
                    .until(() -> APPLIED_OR_IN_PROGRESS.contains(actualChangeStatus));
            softly.assertThat(actualChangeStatus).as("Unexpected change status for audit result %d of %d", i, finalAuditResults.size())
                    .isIn(APPLIED_OR_IN_PROGRESS);
        }
        ids.forEach(longId -> auditResultsRepository.deleteByExecutionIdAndId(execId, longId));
        softly.assertThat(auditResultsRepository.findAll()).isEmpty();
    }

    @Test
    public void whenApplyAllChangesIsCalled_thenCorrectChangesAreApplied() throws Exception {
        ReflectionTestUtils.setField(ncmpService, "networkCmProxyApi", mockedNetworkCmProxyApi);
        final Execution execution = executionsRepository.save(createDefaultExecution());
        final String executionId = execution.getId().toString();
        AuditResult notAppliedAuditResult = createAuditResult(executionId);
        AuditResult implementationCompleteAuditResult = createAuditResult(executionId);
        AuditResult implementationAbortedAuditResult = createAuditResult(executionId);
        AuditResult implementationFailedAuditResult = createAuditResult(executionId);
        AuditResult reversionCompleteAuditResult = createAuditResult(executionId);

        notAppliedAuditResult.setManagedObjectFdn("SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW," +
                "ManagedElement=LTE06dg2ERBS00005,ENodeBFunction=1,EUtranCellFDD=LTE06dg2ERBS00005-2");

        implementationCompleteAuditResult.setChangeStatus(ChangeStatus.IMPLEMENTATION_COMPLETE);
        implementationAbortedAuditResult.setChangeStatus(ChangeStatus.IMPLEMENTATION_ABORTED);
        implementationFailedAuditResult.setChangeStatus(ChangeStatus.IMPLEMENTATION_FAILED);
        reversionCompleteAuditResult.setChangeStatus(ChangeStatus.REVERSION_COMPLETE);

        auditResultsRepository.saveAllAndFlush(List.of(notAppliedAuditResult, implementationCompleteAuditResult,
                implementationAbortedAuditResult, implementationFailedAuditResult, reversionCompleteAuditResult));

//        List<AuditResult> expectedAuditResults = createAuditResults(executionId);
//        expectedAuditResults.get(0).setManagedObjectFdn("SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW," +
//                "ManagedElement=LTE06dg2ERBS00005,ENodeBFunction=1,EUtranCellFDD=LTE06dg2ERBS00005-2");
//        expectedAuditResults.get(0).setChangeStatus(ChangeStatus.IMPLEMENTATION_COMPLETE);
//        auditResultsRepository.saveAll(expectedAuditResults);

        doNothing().when(ncmpService).patchManagedObjects(any(), any(), any());

        final String requestBody = """
                {
                    "auditResultIds": [],
                    "approveForAll" : true,
                     "operation"     : "APPLY_CHANGE" 
                }""";

        final List<AuditResult> auditResults = auditResultsRepository.findByExecutionId(executionId).get();
        assertThat(auditResults.get(0).getChangeStatus()).isEqualTo(ChangeStatus.NOT_APPLIED);
        assertThat(auditResults.get(1).getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_COMPLETE);
        assertThat(auditResults.get(2).getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_ABORTED);
        assertThat(auditResults.get(3).getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_FAILED);
        assertThat(auditResults.get(4).getChangeStatus()).isEqualTo(ChangeStatus.REVERSION_COMPLETE);

        mvc.perform(post(VERSION_PATH + "/executions/" + executionId + "/audit-results")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, TOKEN)
                .content(requestBody))
                .andExpect(status().isAccepted());

        await().alias("Waiting for change implementation to complete").atMost(10, SECONDS)
                .pollInterval(1, SECONDS)
                .until(isChangeImplementationComplete(executionId));

        verify(ncmpService, times(2)).patchManagedObjects(any(), any(), any());

        final List<AuditResult> actualAuditResults = auditResultsRepository.findByExecutionId(executionId).get();
        assertThat(actualAuditResults.get(0).getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_COMPLETE);
        assertThat(actualAuditResults.get(1).getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_COMPLETE);
        assertThat(actualAuditResults.get(2).getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_COMPLETE);
        assertThat(actualAuditResults.get(3).getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_COMPLETE);
        assertThat(actualAuditResults.get(4).getChangeStatus()).isEqualTo(ChangeStatus.REVERSION_COMPLETE);
    }

    @Test
    public void validateWhenSearchCMHandleIdsCallFails_thenCorrectNumberOfRetriesBasedOnMaxAttempts() {
        ReflectionTestUtils.setField(ncmpService, "networkCmProxyApi", mockedNetworkCmProxyApi);
        final List<String> moduleRevisionList = List.of("ericsson-enm-Lrat", "ericsson-enm-GNBDU", "ericsson-enm-GNBCUCP", "ericsson-enm-GNBCUUP");
        when(mockedNetworkCmProxyApi.searchCmHandleIds(any(CmHandleQueryParameters.class))).thenThrow(CompletionException.class);

        assertThatThrownBy(() -> ncmpService.getCMHandleModuleRevisions(moduleRevisionList))
                .isInstanceOf(CMServiceException.class)
                .hasMessage("Error encountered while retrieving CMHandle module revisions from NCMP.");

        final int expectedRetryAttempts = moduleRevisionList.size() * modelDiscoveryMaxRetryAttempts;
        verify(mockedNetworkCmProxyApi, atLeast(expectedRetryAttempts - 1))
                .searchCmHandleIds(any(CmHandleQueryParameters.class));
        verify(mockedNetworkCmProxyApi, atMost(expectedRetryAttempts))
                .searchCmHandleIds(any(CmHandleQueryParameters.class));
    }

    @Test
    public void validateWhenPatchMOCallFails_thenCorrectNumberOfRetriesBasedOnMaxAttempts() {
        ReflectionTestUtils.setField(ncmpService, "networkCmProxyApi", mockedNetworkCmProxyApi);

        when(mockedNetworkCmProxyApi
                .patchResourceDataRunningForCmHandle(any(), any(), any(), any(), any()))
                        .thenThrow(RestClientException.class);

        assertThatThrownBy(() -> ncmpService.patchManagedObject(createAuditResults("1").get(0)))
                .isInstanceOf(CMServiceException.class)
                .hasMessage(
                        "Failed to apply change in NCMP for FDN: SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW," +
                                "ManagedElement=LTE06dg2ERBS00005,ENodeBFunction=1,EUtranCellFDD=LTE06dg2ERBS00005-1.");

        verify(mockedNetworkCmProxyApi, times(ncmpCallsMaxRetryAttempts)).patchResourceDataRunningForCmHandle(any(), any(), any(), any(), any());
    }

    @Test
    public void whenExecutionsAreCleanedUp_thenExecutionStatusesAreUpdatedCorrectly() {
        final Execution auditSuccessfulExecution = createDefaultExecution();
        final Execution auditPartiallySuccessfulExecution = createDefaultExecution();
        final Execution auditInProgressExecution = createDefaultExecution();
        final Execution implementationsPartiallySuccessfulExecution = createDefaultExecution();
        final Execution implementationsInProgressExecution = createDefaultExecution();
        final Execution reversionPartiallySuccessfulExecution = createDefaultExecution();
        final Execution reversionInProgressExecution = createDefaultExecution();

        auditSuccessfulExecution.setJobName(JOBNAME_1);
        auditPartiallySuccessfulExecution.setJobName(JOBNAME_1);
        auditInProgressExecution.setJobName(JOBNAME_1);
        implementationsPartiallySuccessfulExecution.setJobName(JOBNAME_1);
        implementationsInProgressExecution.setJobName(JOBNAME_1);
        reversionPartiallySuccessfulExecution.setJobName(JOBNAME_1);
        reversionInProgressExecution.setJobName(JOBNAME_1);

        auditPartiallySuccessfulExecution.setExecutionStatus(ExecutionStatus.AUDIT_PARTIALLY_SUCCESSFUL);
        auditInProgressExecution.setExecutionStatus(ExecutionStatus.AUDIT_IN_PROGRESS);
        implementationsPartiallySuccessfulExecution.setExecutionStatus(ExecutionStatus.CHANGES_PARTIALLY_SUCCESSFUL);
        implementationsInProgressExecution.setExecutionStatus(ExecutionStatus.CHANGES_IN_PROGRESS);
        reversionPartiallySuccessfulExecution.setExecutionStatus(REVERSION_PARTIALLY_SUCCESSFUL);
        reversionInProgressExecution.setExecutionStatus(ExecutionStatus.REVERSION_IN_PROGRESS);

        assertThat(executionsRepository.findByJobName(JOBNAME_1).get()).isEmpty();
        executionsRepository.saveAll(List.of(auditSuccessfulExecution, auditPartiallySuccessfulExecution,
                auditInProgressExecution, implementationsPartiallySuccessfulExecution, implementationsInProgressExecution,
                reversionPartiallySuccessfulExecution, reversionInProgressExecution));
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get()).hasSize(7);

        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(0).getExecutionStatus()).isEqualTo(ExecutionStatus.AUDIT_SUCCESSFUL);
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(1).getExecutionStatus())
                .isEqualTo(ExecutionStatus.AUDIT_PARTIALLY_SUCCESSFUL);
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(2).getExecutionStatus()).isEqualTo(ExecutionStatus.AUDIT_IN_PROGRESS);
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(3).getExecutionStatus())
                .isEqualTo(ExecutionStatus.CHANGES_PARTIALLY_SUCCESSFUL);
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(4).getExecutionStatus()).isEqualTo(ExecutionStatus.CHANGES_IN_PROGRESS);
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(5).getExecutionStatus())
                .isEqualTo(REVERSION_PARTIALLY_SUCCESSFUL);
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(6).getExecutionStatus()).isEqualTo(ExecutionStatus.REVERSION_IN_PROGRESS);
        executionService.cleanUpExecutionsOnStartUp(applicationReadyEvent);
        await().alias("Waiting for execution clean up to finish.").atMost(20, SECONDS).pollInterval(1, SECONDS)
                .until(isExecutionCleanUpComplete());
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(0).getExecutionStatus()).isEqualTo(ExecutionStatus.AUDIT_SUCCESSFUL);
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(1).getExecutionStatus())
                .isEqualTo(ExecutionStatus.AUDIT_PARTIALLY_SUCCESSFUL);
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(2).getExecutionStatus()).isEqualTo(ExecutionStatus.AUDIT_ABORTED);
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(3).getExecutionStatus())
                .isEqualTo(ExecutionStatus.CHANGES_PARTIALLY_SUCCESSFUL);
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(4).getExecutionStatus()).isEqualTo(ExecutionStatus.CHANGES_ABORTED);
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(5).getExecutionStatus())
                .isEqualTo(REVERSION_PARTIALLY_SUCCESSFUL);
        assertThat(executionsRepository.findByJobName(JOBNAME_1).get().get(6).getExecutionStatus()).isEqualTo(ExecutionStatus.REVERSION_ABORTED);

    }

    @Test
    void whenAuditResultsAreCleanedUp_thenChangeStatusesAreUpdatedCorrectly() {
        final AuditResult defaultAuditResult = createAuditResult("1");
        final AuditResult implementationInProgressAuditResult = createAuditResult("1");
        final AuditResult implementationCompleteAuditResult = createAuditResult("1");
        final AuditResult reversionInProgressAuditResult = createAuditResult("1");
        final AuditResult reversionCompleteAuditResult = createAuditResult("1");

        implementationInProgressAuditResult.setChangeStatus(ChangeStatus.IMPLEMENTATION_IN_PROGRESS);
        implementationCompleteAuditResult.setChangeStatus(ChangeStatus.IMPLEMENTATION_COMPLETE);
        reversionInProgressAuditResult.setChangeStatus(ChangeStatus.REVERSION_IN_PROGRESS);
        reversionCompleteAuditResult.setChangeStatus(ChangeStatus.REVERSION_COMPLETE);

        assertThat(auditResultsRepository.findByExecutionId("1").get()).isEmpty();
        auditResultsRepository.saveAll(List.of(defaultAuditResult, implementationInProgressAuditResult,
                implementationCompleteAuditResult, reversionInProgressAuditResult, reversionCompleteAuditResult));
        assertThat(auditResultsRepository.findByExecutionId("1").get()).hasSize(5);

        assertThat(auditResultsRepository.findByExecutionId("1").get().get(0).getChangeStatus()).isEqualTo(ChangeStatus.NOT_APPLIED);
        assertThat(auditResultsRepository.findByExecutionId("1").get().get(1).getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_IN_PROGRESS);
        assertThat(auditResultsRepository.findByExecutionId("1").get().get(2).getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_COMPLETE);
        assertThat(auditResultsRepository.findByExecutionId("1").get().get(3).getChangeStatus()).isEqualTo(ChangeStatus.REVERSION_IN_PROGRESS);
        assertThat(auditResultsRepository.findByExecutionId("1").get().get(4).getChangeStatus()).isEqualTo(ChangeStatus.REVERSION_COMPLETE);
        auditResultService.cleanUpAuditResultsOnStartUp();
        await().alias("Waiting for audit result clean up to finish").atMost(20, SECONDS).pollInterval(1, SECONDS)
                .until(isAuditResultCleanUpComplete());
        assertThat(auditResultsRepository.findByExecutionId("1").get().get(0).getChangeStatus()).isEqualTo(ChangeStatus.NOT_APPLIED);
        assertThat(auditResultsRepository.findByExecutionId("1").get().get(1).getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_ABORTED);
        assertThat(auditResultsRepository.findByExecutionId("1").get().get(2).getChangeStatus()).isEqualTo(ChangeStatus.IMPLEMENTATION_COMPLETE);
        assertThat(auditResultsRepository.findByExecutionId("1").get().get(3).getChangeStatus()).isEqualTo(ChangeStatus.REVERSION_ABORTED);
        assertThat(auditResultsRepository.findByExecutionId("1").get().get(4).getChangeStatus()).isEqualTo(ChangeStatus.REVERSION_COMPLETE);

    }

    public void assert400ResponseFromCallWithJsonTitleDetailAndGeneralMatchers(final ResultActions mvcCall, final String title,
            final Matcher<String> detailMatcher, final ResultMatcher... matchers) throws Exception {
        mvcCall.andExpectAll(
                status().isBadRequest(),
                content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                jsonPath(JSON_TITLE, is(title)),
                jsonPath(JSON_STATUS, is(BAD_REQUEST)),
                jsonPath(JSON_DETAIL, detailMatcher))
                .andExpectAll(matchers);
    }

    private AuditResult createAuditResult(final String executionId) {
        final AuditResult auditResult = new AuditResult();
        auditResult.setManagedObjectFdn("SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW," +
                "ManagedElement=LTE06dg2ERBS00005,ENodeBFunction=1,EUtranCellFDD=LTE06dg2ERBS00005-1");
        auditResult.setManagedObjectType("EUTranCellFDD");
        auditResult.setAttributeName("attrName");
        auditResult.setCurrentValue("2");
        auditResult.setPreferredValue("3");
        auditResult.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult.setRuleId("1");
        auditResult.setExecutionId(executionId);
        auditResult.setChangeStatus(ChangeStatus.NOT_APPLIED);
        return auditResult;
    }

    private List<AuditResult> createAuditResults(final String executionId) {
        final AuditResult auditResult1 = new AuditResult();
        auditResult1.setManagedObjectFdn("SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW," +
                "ManagedElement=LTE06dg2ERBS00005,ENodeBFunction=1,EUtranCellFDD=LTE06dg2ERBS00005-1");
        auditResult1.setManagedObjectType("EUTranCellFDD");
        auditResult1.setAttributeName("attrName");
        auditResult1.setCurrentValue("2");
        auditResult1.setPreferredValue("3");
        auditResult1.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult1.setRuleId("1");
        auditResult1.setExecutionId(executionId);
        auditResult1.setChangeStatus(ChangeStatus.NOT_APPLIED);
        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setManagedObjectFdn("SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW," +
                "ManagedElement=LTE06dg2ERBS00005,ENodeBFunction=1,EUtranCellFDD=LTE06dg2ERBS00005-1");
        auditResult2.setManagedObjectType("EUTranCellFDD");
        auditResult2.setAttributeName("attrName2");
        auditResult2.setCurrentValue("7");
        auditResult2.setPreferredValue("9");
        auditResult2.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult2.setRuleId("1");
        auditResult2.setExecutionId(executionId);
        auditResult2.setChangeStatus(ChangeStatus.NOT_APPLIED);
        return List.of(auditResult1, auditResult2);
    }

    private Execution createDefaultExecution() {
        return new Execution(0L, "", "", "", ExecutionType.OPEN_LOOP,
                getCurrentUtcDateTime(), getCurrentUtcDateTime(), getCurrentUtcDateTime(), getCurrentUtcDateTime(),
                ExecutionStatus.AUDIT_SUCCESSFUL, 0, 0, 0, 0, 0);
    }

    private Callable<Boolean> isChangeImplementationComplete(String executionId) { //NOPMD method named like it returns boolean
        return () -> {
          final Optional<List<AuditResult>> auditResults = auditResultsRepository.findByExecutionId(executionId);
          final Execution execution = executionsRepository.findById(Long.parseLong(executionId)).orElseThrow();
          return execution.getExecutionStatus() == ExecutionStatus.CHANGES_SUCCESSFUL;
        };
    }

    private Callable<Boolean> isExecutionComplete() { //NOPMD method named like it returns boolean
        return () -> {
            final List<Execution> executionList = executionsRepository.findAll();
            log.info("execution list: {} ", executionList);
            final List<AuditResult> auditResultList = auditResultsRepository.findAll();
            log.info("AuditResult list: {} ", auditResultList);
            return (!executionList.isEmpty() && (//
            ExecutionStatus.AUDIT_SUCCESSFUL.equals(executionList.get(0).getExecutionStatus()) ||
                    ExecutionStatus.AUDIT_FAILED.equals(executionList.get(0).getExecutionStatus())));
        };
    }

    private Callable<Boolean> isExecutionCleanUpComplete() { //NOPMD method named like it returns boolean
        return () -> {
            final List<Execution> executions = executionsRepository.findByJobName(JOBNAME_1).get();

            return (!executions.isEmpty() && (executions.get(2).getExecutionStatus().equals(ExecutionStatus.AUDIT_ABORTED) ||
                    executions.get(4).getExecutionStatus().equals(ExecutionStatus.CHANGES_ABORTED) ||
                    executions.get(6).getExecutionStatus().equals(ExecutionStatus.REVERSION_ABORTED)));
        };
    }

    private Callable<Boolean> isAuditResultCleanUpComplete() { //NOPMD method named like it returns boolean
        return () -> {
            final List<AuditResult> auditResults = auditResultsRepository.findByExecutionId("1").get();
            return (!auditResults.isEmpty() && (auditResults.get(1).getChangeStatus().equals(ChangeStatus.IMPLEMENTATION_ABORTED) ||
                    auditResults.get(3).getChangeStatus().equals(ChangeStatus.REVERSION_ABORTED)));
        };
    }

    private Callable<Boolean> changesApplied() {
        return () -> {
            final List<Execution> executionList = executionsRepository.findAll();
            log.info("execution list: {} ", executionList);
            if (executionList.isEmpty()) {
                return false;
            }
            final Execution execution = executionList.get(0);
            return ((ExecutionStatus.CHANGES_SUCCESSFUL.equals(execution.getExecutionStatus()) ||
                    ExecutionStatus.CHANGES_FAILED.equals(execution.getExecutionStatus()) ||
                    ExecutionStatus.CHANGES_PARTIALLY_SUCCESSFUL.equals(execution.getExecutionStatus())));
        };
    }

    private Callable<Boolean> reversionApplied() {
        return () -> {
            final List<Execution> executionList = executionsRepository.findAll();
            log.info("execution list: {} ", executionList);
            if (executionList.isEmpty()) {
                return false;
            }
            final Execution execution = executionList.get(0);
            return ((ExecutionStatus.REVERSION_SUCCESSFUL.equals(execution.getExecutionStatus()) ||
                    ExecutionStatus.REVERSION_FAILED.equals(execution.getExecutionStatus()) ||
                    REVERSION_PARTIALLY_SUCCESSFUL.equals(execution.getExecutionStatus())));
        };
    }

    private String getSchedule(final LocalDateTime dateTime) {
        return String.format("%s %s %s %s %s %s", dateTime.getSecond(),
                dateTime.getMinute(), dateTime.getHour(), dateTime.getDayOfMonth(),
                dateTime.getMonthValue(), dateTime.getDayOfWeek().getValue());
    }

    private Map<String, List<Object>> getRawManagedObjectsForAudit() {
        final var eUtranCellFddAttr = Map.of(CELL_CAP_MIN_CELL_SUB_CAP, "100",
                CELL_CAP_MIN_MAX_WRI_PROT, "true",
                CFRA_ENABLE, "false");

        return getRawManagedObjects(eUtranCellFddAttr, eUtranCellFddAttr);
    }

    private Map<String, List<Object>> getRawManagedObjectsForReversionValidation() {
        return getRawManagedObjects(Map.of(CFRA_ENABLE, "false"), Map.of(CELL_CAP_MIN_CELL_SUB_CAP, "1000"));
    }

    private Map<String, List<Object>> getRawManagedObjects(final Map<String, String> cellFdd1Attr, final Map<String, String> cellFdd2Attr) {
        return Map.of(MANAGED_ELEMENT, List.of(
                Map.of("id", "LTE416dg2ERBS00006",
                        ATTRIBUTES, Map.of(DN_PREFIX, "SubNetwork=Europe,SubNetwork=Ireland,SubNetwork=NETSimW"),
                        ENODEBFUNCTION, List.of(
                                Map.of("id", "1",
                                EUTRAN_CELL_FDD, List.of(
                                getRawEUtranCellFdd("LTE06dg2ERBS00030-1", cellFdd1Attr),
                                getRawEUtranCellFdd("LTE06dg2ERBS00030-3", cellFdd2Attr)))))));
    }

    private Map<String, Object> getRawEUtranCellFdd(final String id, final Map<String, String> attributes) {
        return Map.of("id", id, ATTRIBUTES, attributes);
    }

    private void verifyMetrics(final String metricOutput, final String... metricNames) {
        final List<String> metrics = Arrays.stream(metricNames)
                .map(metricName -> metricName.replace(".", "_"))
                .toList();
        assertThat(metricOutput).contains(metrics);
    }

    //TODO add test for deleting rulesets
    //TODO add test for deleting scopes

    private void generateModels() throws FileNotFoundException, ModelParserException {
        final List<CMHandleModuleRevision> moduleRevisions = List.of(
                new CMHandleModuleRevision(CM_HANDLE, "ericsson-enm-Lrat", "2351-11-28"),
                new CMHandleModuleRevision(CM_HANDLE, "ericsson-enm-GNBCUCP", "2351-11-28"),
                new CMHandleModuleRevision(CM_HANDLE, "ericsson-enm-GNBDU", "2351-11-28"),
                new CMHandleModuleRevision(CM_HANDLE, "ericsson-enm-GNBCUUP", "2351-11-28"));
        final Models models = modelParser.generateModel(moduleRevisions, ResourceUtils.getFile("classpath:" + "model_parser_test_files/modules"));
        modelManager.setModels(models);
        modelManager.setCmHandleModuleRevisionList(moduleRevisions);
    }
}