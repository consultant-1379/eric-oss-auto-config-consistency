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

import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
    public static final String EMPTY_STRING = "";
    public static final String EQUAL = "=";
    public static final String COMMA = ",";
    public static final String COLON = ":";
    public static final String SEP = "/";

    public static final String SEMICOLON = ";";
    public static final String UTC = "UTC";

    // REST constants
    public static final String VERSION = "v1";
    public static final String VERSION_PATH = SEP + VERSION;
    public static final String APPLICATION_JSON = "application/json";

    //RAN Terms
    public static final String DN_PREFIX = "dnPrefix";
    public static final String MANAGED_ELEMENT = "ManagedElement";
    public static final String MANAGEDELEMENT_FWD_SLASH_ATTRIBUTES = "ManagedElement/attributes(";
    public static final String MOC_HYPHEN = "moc-";

    // Server errors
    public static final String SERVER_ERROR = "Server Error";
    public static final String UNEXPECTED_ERROR = "Unexpected error";

    //Regex RulesetController, JobService
    public static final String NAME_REGEX = "^[a-z0-9_-]+$";
    public static final Pattern NAME_PATTERN = Pattern.compile(NAME_REGEX);
    public static final Integer NAME_MAX_LENGTH = 255;
    public static final Integer CONDITION_MAX_LENGTH = 512;
    public static final int EXECUTION_AND_RULESET_NAME_AND_JOB_ID_MAX_LENGTH = 100;
    public static final Integer NODE_FDN_MAX_LENGTH = 1600;

    // Validation
    public static final String FAILED_TO_CREATE_RULESET = "Failed to create ruleset.";
    public static final String FAILED_TO_UPDATE_RULESET = "Failed to update ruleset.";
    public static final String FAILED_TO_CREATE_SCOPE = "Failed to create scope.";
    public static final String DATABASE_OPERATION_FAILED = "Database operation failed.";
    public static final String INVALID_CHANGE_OPERATION = "Invalid operation. Operation must be APPLY_CHANGE or REVERT_CHANGE.";
    public static final String VALIDATION_FAILED = "Validation failed";
    public static final String VALIDATION_COMPLETE = "Validation complete";
    public static final String INVALID_JOBNAME_FOR_DELETE_OR_UPDATE = "Invalid resource identifier in url: " +
            "Only lower case alphanumeric, underscores and dashes allowed, with between 4 -100 characters.";
    public static final String EXISTING_JOBNAME_ERROR = "Invalid jobName: job already exists";
    public static final String NONEXISTENT_JOBNAME_ERROR = "Invalid jobName: jobName does not exist";
    public static final String BLANK_JOBNAME_ERROR = "Invalid jobName: Must not be blank";
    public static final String REGEX_JOBNAME_ERROR = "Invalid jobName: Only lower case alphanumeric, underscores and dashes allowed";
    public static final String JOBNAME_LENGTH_ERROR = "Invalid jobName: Must be of 4 - 100 characters";
    public static final String REGEX_RULESET_JOBNAME_ERROR = String.format(
            "Invalid rulesetName: Use only lower case alphanumeric, underscores and dashes up to a maximum of %s",
            EXECUTION_AND_RULESET_NAME_AND_JOB_ID_MAX_LENGTH);
    public static final String BLANK_SCOPENAME_ERROR = "Invalid scopeName: Must not be blank";
    public static final String BLANK_SCHEDULE_ERROR = "Invalid schedule: Must not be blank";
    public static final String INVALID_CRON_SCHEDULE_ERROR = "Invalid schedule: Cron expression not valid";
    public static final String NONEXISTENT_RULESET_ERROR = "Invalid Ruleset: Ruleset does not exist";
    public static final String BLANK_RULESET_ERROR = "Invalid rulesetName: Must not be blank";
    public static final Integer MAX_RULES = 100;
    public static final String MAX_RULES_EXCEEDED = String.format("Invalid Ruleset: Max %d Rules Supported", MAX_RULES);
    public static final String NONEXISTENT_SCOPE_ERROR = "Invalid Scope: Scope does not exist";
    public static final String REGEX_SCOPE_JOBNAME_ERROR = String.format(
            "Invalid scopeName: Use only lower case alphanumeric, '-' and '_' characters up to a maximum of %s.", NAME_MAX_LENGTH);

    public static final String PROPOSED_IDS_DONT_EXIST = "No inconsistencies found for proposed change id(s)";

    public static final String PROVIDE_EXECUTION_ID = "Provide a valid Execution ID.";
    public static final String EXECUTION_ID_DOES_NOT_EXIST = "Execution ID does not exist.";

    public static final String PROVIDE_A_VALID_RULESET_ID = "Provide a valid Ruleset ID.";
    public static final String UUID_ID_ERROR = "Error parsing ID. Provided ID is invalid.";
    //RuleSetService URI
    public static final String RULESETS_URI = VERSION + "/rulesets/";
    //ScopeService URI
    public static final String SCOPES_URI = VERSION + "/scopes/";

    //Csv content type
    public static final String ATTACHMENT = "attachment";
    public static final String CSV_CONTENT_TYPE = "text/csv";
    public static final String CSV_FILENAME_TEMPLATE = "%s.csv";

    public static final String INVALID_UUID = "invalid-uuid";

    //Audit results
    public static final String FDN_REGEX = "[A-Za-z0-9,.%!&=_?:\\-\\/\\s]+";
    public static final Pattern FDN_PATTERN = Pattern.compile(FDN_REGEX);
    public static final String INVALID_FILTER_VALUE_MESSAGE = "Invalid filter value. The allowed characters for values, are as follows:" +
            "'A-Z', 'a-z', '0-9', '-' , '_', '/' , ',', '.' , '%' , '&', '!', '?', ' ' (space) and ':'";
    public static final String INVALID_PAGE_MESSAGE = "Invalid page number: Must not be a negative value.";
    public static final String INVALID_PAGESIZE_MESSAGE = "Invalid pageSize: Use only numeric values of 1 or more.";

    //NCMP Access
    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String NCMP = "ncmp";
    public static final String NCMP_DATASTORE_PASSTHROUGH_OPERATIONAL = "ncmp-datastore:passthrough-operational";
    public static final String NCMP_DATASTORE_PASSTHROUGH_RUNNING = "ncmp-datastore:passthrough-running";

    //IAM Access
    public static final String IAM = "iam";
    public static final String CLIENT_CREDENTIALS = "client_credentials";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String JSON_PROPERTY_ACCESS_TOKEN = "access_token";
    public static final String JSON_PROPERTY_EXPIRES_IN = "expires_in";

    //ThreadPoolScheduler
    public static final String OSS_TIME_ZONE = "UTC";
    public static final String FACILITY_KEY = "facility";
    public static final String NON_AUDIT_LOG = "security/authorization messages";

    //LogControlFileWatcher
    public static final int RELOAD_LOG_CONTROL_FILE_SCHEDULE = 40000;

    //Connection
    public static final int MAX_TOTAL_CONNECTIONS_PER_ROUTE = 20;
    public static final int MAX_TOTAL_CONNECTIONS = 50;
    public static final int CONNECT_TIMEOUT = 30;
    public static final int REQUEST_TIMEOUT = 10;
    public static final int SOCKET_TIMEOUT = 180;
    public static final int CONNECTION_TIME_TO_LIVE_SECONDS = 30;

    public static final String STANDARD_COOKIE_SPEC = "standard";

    // Cert files update check schedule
    public static final int CERT_FILE_CHECK_SCHEDULE_IN_SECONDS = 100;
    public static final int CERT_FILE_CHECK_INITIAL_DELAY_IN_SECONDS = 30;

    public static final String FDN = "FDN";

    public static final String CHANGES_IN_PROGRESS = "Cannot apply/revert changes with status 'Implementation in progress'";

    public static final String REVERSIONS_IN_PROGRESS = "Cannot apply/revert changes with status 'Reversions in progress'";


    public static final String SUBJECT_KEY = "subject";
    public static final String AUDIT_LOG = "log audit";
    public static final String PREVIOUS_JOB_EXECUTION = "Changes can only be reverted from the latest execution";
    public static final String FILTER_NAME_FDN = "managedObjectFdn";
}
