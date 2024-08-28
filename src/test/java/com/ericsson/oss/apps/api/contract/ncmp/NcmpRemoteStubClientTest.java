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

package com.ericsson.oss.apps.api.contract.ncmp;

import static com.ericsson.assertions.response.ResponseEntityAssertions.assertThat;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import com.ericsson.oss.apps.util.Utilities;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureStubRunner(repositoryRoot = "https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-release-local", stubsMode = StubRunnerProperties.StubsMode.REMOTE, ids = "com.ericsson.oss.internaltools.stub:eric-oss-ncmp-stub:+:stubs:9092")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class NcmpRemoteStubClientTest {
    //TODO: IDUN-88737 Implement NCMP contract for modules/definitions endpoint

    @Autowired
    private RestTemplate restTemplate;

    @Test
    void get_response_from_remote_stub_ncmp_nrcellcu_endpoint() throws IOException {
        final String ncmpUrl = "ncmp/v1/ch/CMHANDLE/data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=/&options=fields=NRCellCU/attributes(cellLocalId)";

        final ResponseEntity<String> response = Utilities.executeHttpGet(restTemplate, createUrl(ncmpUrl), HttpHeaders.EMPTY);

        assertThat(response).hasStatus(HttpStatus.OK)
                .hasJsonBody(Utilities.readFileFromResources("json/verification/ncmp/nrCellCu.min.json"), JSONCompareMode.LENIENT);
    }

    @Test
    void get_response_from_remote_stub_ncmp_nrcelldu_endpoint() throws IOException {
        final String ncmpUrl = "ncmp/v1/ch/CMHANDLE/data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=/ericsson-enm-ComTop:ManagedElement[@id=1]/ericsson-enm-GNBDU:GNBDUFunction[@id=16]/ericsson-enm-nrcelldu:NRCellDU[@id=001]&options=fields=ericsson-enm-addPlmnInfo:AdditionalPLMNInfo/attributes(pLMNIdList)";

        final ResponseEntity<String> response = Utilities.executeHttpGet(restTemplate, createUrl(ncmpUrl), HttpHeaders.EMPTY);
        assertThat(response).hasStatus(HttpStatus.OK)
                .hasJsonBody(Utilities.readFileFromResources("json/verification/ncmp/nrCellDu.min.json"), JSONCompareMode.LENIENT);
    }

    @Test
    void get_cmHandles_endpoint_using_idsearches_endpoint_returns_cmhandle_list() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");

        final String ncmpUrl = "ncmp/v1/ch/id-searches";
        final String body = "{\"cmHandleQueryParameters\": [{\"conditionName\": \"hasAllModules\",\"conditionParameters\": [{\"moduleName\": \"ericsson-enm-GNBDU\"}]}]}";
        final ResponseEntity<String> response = Utilities.executeHttpPost(restTemplate, createUrl(ncmpUrl), headers, body);

        assertThat(response).hasStatus(HttpStatus.OK)
                .hasBody("[\"C8FDAD6D077E25C17983E712EABCF63D\",\"8167851EDC6A685A4E0434CFDA68C8A3\"]");
    }

    @Test
    void get_modules_endpoint_returns_modulename_and_revision_list() throws IOException {
        final String ncmpUrl = "ncmp/v1/ch/CMHANDLE/modules";
        final ResponseEntity<String> response = Utilities.executeHttpGet(restTemplate, createUrl(ncmpUrl), HttpHeaders.EMPTY);

        assertThat(response).hasStatus(HttpStatus.OK)
                .hasJsonBody(Utilities.readFileFromResources("json/verification/ncmp/modules"), JSONCompareMode.LENIENT);
    }

    @Test
    void patch_changes_endpoint_returns_ok() {
        final String ncmpUrl = "ncmp/v1/ch/A9E66852689BEAC37624CEE4A1DD1598/data/ds/ncmp-datastore:passthrough-running";
        final String query = "?resourceIdentifier=ericsson-enm-ComTop:ManagedElement[@id=LTE68dg2ERBS00079]/ericsson-enm-GNBDU:GNBDUFunction[@id=1]";
        final String body = """
                {
                  "NRCellDU": [
                    {
                      "id": "NR45gNodeBRadio00022-99",
                      "attributes": {
                        "pLMNIdList": [{"mcc":"328", "mnc":"49"}, {"mcc":"329", "mnc":"50"}],
                        "sNSSAIList": [{"sd":"124", "sst":"124"}, {"sd":"125", "sst":"125"}],
                        "sibType2": {"siPeriodicity":"16", "siBroadcastStatus":"BROADCASTING"}
                      }
                    }
                  ]
                }""";
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        final ResponseEntity<String> response = Utilities.executeHttpPatch(restTemplate, createUrl(ncmpUrl + query), headers, body);

        assertThat(response).hasStatus(HttpStatus.OK);
    }

    @Test
    void patch_duplicate_changes_endpoint_returns_ok() {
        final String ncmpUrl = "ncmp/v1/ch/A9E66852689BEAC37624CEE4A1DD1598/data/ds/ncmp-datastore:passthrough-running";
        final String query = "?resourceIdentifier=ericsson-enm-ComTop:ManagedElement[@id=LTE68dg2ERBS00079]/ericsson-enm-GNBDU:GNBDUFunction[@id=1]";
        final String body = """
                {
                  "NRCellDU": [
                    {
                      "id": "NR45gNodeBRadio00022-99",
                      "attributes": {
                        "pLMNIdList": [{"mcc":"328", "mnc":"49"}, {"mcc":"329", "mnc":"50"}],
                        "sNSSAIList": [{"sd":"124", "sst":"124"}, {"sd":"125", "sst":"125"}],
                        "sibType2": {"siPeriodicity":"16", "siBroadcastStatus":"BROADCASTING"}
                      }
                    }
                  ]
                }""";
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        Utilities.executeHttpPatch(restTemplate, createUrl(ncmpUrl + query), headers, body);

        //apply a change again returns the same response
        final ResponseEntity<String> response = Utilities.executeHttpPatch(restTemplate, createUrl(ncmpUrl + query), headers, body);

        assertThat(response).hasStatus(HttpStatus.OK);
    }

    private static URI createUrl(final String url) {
        return URI.create(String.format("http://localhost:9092/%s", url));
    }
}
