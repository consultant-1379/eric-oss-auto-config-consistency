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

package com.ericsson.oss.apps.util;

import static com.ericsson.oss.apps.util.Constants.VERSION;
import static com.ericsson.oss.apps.util.Constants.VERSION_PATH;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.ericsson.oss.apps.executor.ChangeStatus;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TestDefaults {

    // EE8A3CDAD92C0EF57DC8E47371C85DDB is the cmHandle of: SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030,
    // ManagedElement=NR03gNodeBRadio00030
    public static final String MANAGED_ELEMENT_1_CM_HANDLE = "EE8A3CDAD92C0EF57DC8E47371C85DDB";
    public static final String MANAGED_ELEMENT_1_FDN = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030," +
            "ManagedElement=NR03gNodeBRadio00030";
    public static final String MANAGED_ELEMENT_2_CM_HANDLE = "9F34B7297172B2663A428091BD8A9591";
    public static final String MANAGED_ELEMENT_2_FDN = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00031," +
            "ManagedElement=NR03gNodeBRadio00031";

    public static final String FDN_EXAMPLE_WITH_ENODEBFUNC = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030," +
            "ManagedElement=NR03gNodeBRadio00030,ENodeBFunction=1";
    public static final String FDN_EXAMPLE_WITH_NRCELL = "SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00030," +
            "ManagedElement=NR03gNodeBRadio00030,NRCellCU=NR03gNodeBRadio00030-1";
    public static final String DEFAULT_JOB_NAME = "all-ireland-1";
    public static final String JOBS_URL = VERSION_PATH + "/jobs";
    public static final String DEFAULT_RULESET_NAME = "test_ruleset";
    public static final String DEFAULT_SCOPE_NAME = "test_scope";
    public static final String DEFAULT_RULESET_NAME_TWO = "test_ruleset2";
    public static final String DEFAULT_FILE_NAME = "fileName";
    public static final String ENODEBFUNCTION = "ENodeBFunction";

    public static final String END_TO_END_RULESET_PATH = "src/test/resources/EndToEndValidRuleset.csv";
    public static final String END_TO_END_SCOPE_PATH = "src/test/resources/EndToEndValidScope.csv";
    public static final String END_TO_END_UPDATED_SCOPE_PATH = "src/test/resources/EndToEndUpdatedScope.csv";
    public static final String END_TO_END_UPDATED_RULESET_PATH = "src/test/resources/EndToEndUpdatedRuleset.csv";
    public static final String INCORRECT_COLUMNS_RULESET_PATH = "src/test/resources/IncorrectColumnsRuleset.csv";
    public static final String INVALID_MO_RULESET_PATH = "src/test/resources/InvalidMoRuleset.csv";
    public static final String INVALID_ATTRIBUTE_RULESET_PATH = "src/test/resources/InvalidAttributeRuleset.csv";
    public static final String INVALID_ATTRIBUTE_VALUE_RULESET_PATH = "src/test/resources/InvalidAttributeValueRuleset.csv";
    public static final String INVALID_CONDITION_RULESET_PATH = "src/test/resources/InvalidConditionRuleset.csv";

    public static final String TEST_CSV_NAME = "expectedRuleset.csv";
    public static final String CSV_CONTENT_TYPE = "text/csv";
    public static final UUID DEFAULT_UUID = UUID.fromString("3f2504e0-4f89-11d3-9a0c-0305e82c3301");

    public static final String RULESET_URI_EXAMPLE = VERSION + "/rulesets/3f2504e0-4f89-11d3-9a0c-0305e82c3301";
    public static final String RULESETS_URL = VERSION_PATH + "/rulesets";
    public static final String RULESET_ID_EXAMPLE = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";

    public static final String SCOPE_ATHLONE_ID = "13aa6e40-a9e2-4fa2-9468-16c4797a5ca0";
    public static final String SCOPE_ATHLONE = "athlone";
    public static final UUID SCOPE_ATHLONE_UUID = UUID.fromString(SCOPE_ATHLONE_ID);
    public static final String SCOPES_URL = VERSION_PATH + "/scopes";
    public static final String SLASH = "/";
    public static final String SCOPE_URI_ATHLONE_EXAMPLE = SCOPES_URL.substring(1) + SLASH + SCOPE_ATHLONE_ID;
    public static final String SCOPE_DUBLIN_ID = "13aa6e40-a9e2-4fa2-9468-16c4797a5ca1";
    public static final String SCOPE_DUBLIN = "dublin";
    public static final String SCOPE_URI_DUBLIN_EXAMPLE = SCOPES_URL.substring(1) + SLASH + SCOPE_DUBLIN_ID;

    public static final String VALID_CRON = "0 15 11 ? * *";

    public static final String MANAGED_ELEMENT = "ManagedElement";
    public static final String ENODE_BFUNCTION = "ENodeBFunction";
    public static final String EUTRAN_CELL_FDD = "EUtranCellFDD";
    public static final String INVALID_MO = "invalid";

    public static final String REALLY_LONG_ALPHANUMERIC_STRING = IntStream.range(1, 500)
            .mapToObj(String::valueOf)
            .collect(Collectors.joining());

    // Validation
    public static final int BAD_REQUEST = 400;
    public static final String JSON_TITLE = "$.title";
    public static final String JSON_STATUS = "$.status";
    public static final String JSON_DETAIL = "$.detail";
    public static final String JSON_RULE_VALIDATION_ERRORS = "$.ruleValidationErrors";
    public static final String JSON_RULESET_NAME = "$.rulesetName";
    public static final String JSON_SCOPE_NAME = "$.scopeName";
    public static final String JSON_ID = "$.id";
    public static final String JSON_URI = "$.uri";
    public static final String JSON_SCHEDULE = "$.schedule";
    public static final String JSON_JOB_NAME = "$.jobName";
    public static final String RULESET_NAME_FOR_VALIDATION = "ruleset_name_for_validation";
    public static final String SCOPE_NAME_FOR_VALIDATION = "scope_name_for_validation";
    public static final String VALID_SCHEDULE_FOR_VALIDATION = "0 15 11 ? * *";
    public static final String VALID_JOBNAME_FOR_VALIDATION = "test_validation";
    public static final String RULESET_NAME_PARAM = "rulesetName";
    public static final String SCOPE_NAME_PARAM = "scopeName";
    public static final String TEST_SETUP_ERROR_UNABLE_TO_ACCESS_LOGS = "TEST SETUP ERROR - Unable to access logs";
    public static final String INJECTED_LOG_TO_VALIDATE_TEST = "injectedLogToValidateTest";
    public static final String INJECTED_ERROR_MESSAGE_TO_VALIDATE_TEST = "injectedErrorMessageToValidateTest";
    public static final String FILE = "file";
    public static final String VALIDATION_FAILED_TITLE = "Validation Failed";
    public static final Set<ChangeStatus> APPLIED_OR_IN_PROGRESS = new HashSet<>(
            Set.of(ChangeStatus.IMPLEMENTATION_COMPLETE, ChangeStatus.IMPLEMENTATION_IN_PROGRESS));
    public static final String FILL_IN_ALL_REQUIRED_FIELDS = "Fill in all required fields with the appropriate information.";
    public static final String CFRA_ENABLE = "cfraEnable";
    public static final String CELL_CAP_MIN_CELL_SUB_CAP = "cellCapMinCellSubCap";
    public static final String CELL_CAP_MIN_MAX_WRI_PROT = "cellCapMinMaxWriProt";
    public static final String TEST_SETUP_ERROR = "TEST SETUP ERROR";
    public static final String PRACH_CONFIG_ENABLED = "prachConfigEnabled";
    public static final String ZZZ_TEMPORARY_74 = "zzzTemporary74";
    public static final String SOME_IO_EXCEPTION_MESSAGE = "Some IO exception message";
    public static final String AUTHORIZATION = "authorization";
    public static final String TOKEN = "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI3YXY0YWlHUC0t" +
            "R2swRnRIYnlJa3lXUVJTUjBhcW9peWRVbWpIWENpd1VrIn0.eyJleHAiOjE3MDc0NzE2NTUsImlhdCI6MTcwNzQ3MTM1NSwianRpIjoi" +
            "MDc5MGY1NmItMTNiMS00MjcwLTkwMWMtYmU1Y2RhZGVhNmJlIiwiaXNzIjoiaHR0cHM6Ly9pYW0ua29objAyNC1laWFwLmV3cy5naWMu" +
            "ZXJpY3Nzb24uc2UvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjU4ODU4OWFmLWY4ZTktNDA0Zi1iMDhiL" +
            "WM2OTBjMGZmNTFkNyIsInR5cCI6IkJlYXJlciIsImF6cCI6ImVvIiwic2Vzc2lvbl9zdGF0ZSI6ImJhNzhkMGM0LTJhMmQtNDIzYS1h" +
            "ZDNkLTQ3NjYxY2QxNzk5YiIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cHM6Ly8qIl0sInJlYWxtX2FjY2VzcyI6eyJyb" +
            "2xlcyI6WyJkZWZhdWx0LXJvbGVzLW1hc3RlciIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX" +
            "2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb" +
            "2ZpbGUiXX19LCJzY29wZSI6ImVtYWlsIG9wZW5pZCBwcm9maWxlIiwic2lkIjoiYmE3OGQwYzQtMmEyZC00MjNhLWFkM2QtNDc2NjFjZ" +
            "DE3OTliIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJlYWNjLXVzZXIiLCJnaXZlbl9uYW1lIjoi" +
            "IiwiZmFtaWx5X25hbWUiOiIifQ.MYl7u2ChKmfOVxt-ERoBO66V2ke9uyh6hZjRM73hNO9ZRd9TZGjjKqgRo30JSaaHvemlclI7WE1" +
            "E0j9VbzcMX-dx_j7fDrdLGGDvfB2O2PIv-9aRsWaKBq7sTRZb25XGmIQ47kcj1Ek5R8vL_3VElcnVp40cUUSR9OIb8qrA9XPwF52iKY_r1" +
            "qhSiC8t6SLGV40FhBAP6KbjRNFypB0QWg_olqAw1eaUHJ4L_UjXLNFD7p3jNg_gYA4Pj1IF6YnqAolT8ZKI2JTIQ2s8SzBGGSq1OxQbr" +
            "9mYkTBu_rUkgc5xk758iOTNTc4clTb2t4nN1AuXVJkAG1KT4bItgArzhQ";
    @SuppressWarnings("checkstyle:linelength")
    public static final String INVALID_FDN_TOO_LONG = "%SubNetwork=Europe,SubNetwork=Ireland,MeContext=NR03gNodeBRadio00031,ManagedElement=NR03gNodeBRadio0003100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000%";
    public static final String INVALID_RULESET_NAME_MESSAGE = //
            "Invalid rulesetName: Use only lower case alphanumeric, underscores and dashes up to a maximum of 100";

}
