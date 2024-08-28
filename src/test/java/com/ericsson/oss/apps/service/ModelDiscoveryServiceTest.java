/*******************************************************************************
 * COPYRIGHT Ericsson 2023
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

package com.ericsson.oss.apps.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ericsson.oss.apps.client.ncmp.model.RestModuleDefinition;
import com.ericsson.oss.apps.service.mom.model.Models;
import com.ericsson.oss.apps.service.mom.parser.ModelParser;
import com.ericsson.oss.apps.service.mom.parser.ModelParserException;
import com.ericsson.oss.apps.service.ncmp.model.CMHandleModuleRevision;
import com.ericsson.oss.apps.validation.ModelManagerImpl;

/**
 * Unit tests for {@link ModelDiscoveryService} class.
 */

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@EnableScheduling
public class ModelDiscoveryServiceTest {

    private static final String DUMMY_NCMP_URL = "NCMP-URL";

    private static final List<String> MODULE_NAMES = List.of("ericsson-enm-Lrat", "ericsson-enm-GNBDU", "ericsson-enm-GNBCUCP",
            "ericsson-enm-GNBCUUP");

    private static final String CM_HANDLE_1 = "cmHandle1";

    private static final String CM_HANDLE_2 = "cmHandle2";

    private static final String FILE_CONTENT = "sample module content";

    private ModelDiscoveryService objUnderTest;

    @TempDir
    private File modulesDir;
    private String path;

    @Mock
    private CMService mockNcmpService;

    @Mock
    private Models models;

    @Mock
    private ModelManagerImpl modelManagerImpl;

    @Mock
    private ModelParser modelParser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        path = modulesDir.getPath();
    }

    @Test
    void whenDiscoveringModels_AndCMHandlesHaveIdenticalModules_ThenCorrectYangFilesAreCreated()
            throws CMServiceException, IOException, ModelParserException {
        final ApplicationReadyEvent applicationReadyEvent = mock(ApplicationReadyEvent.class);

        objUnderTest = new ModelDiscoveryService(mockNcmpService, DUMMY_NCMP_URL, modulesDir.getPath() + '/', modelManagerImpl, modelParser);

        when(mockNcmpService.getCMHandleModuleRevisions(MODULE_NAMES)).thenReturn(createIdenticalCmHandleModuleRevisions());
        when(mockNcmpService.getModuleDefinitions(CM_HANDLE_1)).thenReturn(create4RestModuleDefinitions());

        objUnderTest.runOnStartup(applicationReadyEvent);
        await().atMost(1, SECONDS).until(() -> true);

        verify(mockNcmpService, times(1)).getCMHandleModuleRevisions(MODULE_NAMES);
        verify(mockNcmpService, times(1)).getModuleDefinitions(CM_HANDLE_1);
        verify(mockNcmpService, never()).getModuleDefinitions(CM_HANDLE_2);
        verify(modelParser, times(1)).generateModel(anyList(), any());

        assertThat(modulesDir.listFiles()).hasSize(4);

        for (final String expectedFileName : create4ExpectedFileNames()) {
            assertThat(new File(expectedFileName)).exists();
            final Path filePath = Paths.get(expectedFileName);
            final String fileContent = Files.readString(filePath);
            assertThat(fileContent).as("Unexpected content for file:%s", expectedFileName).isEqualTo(FILE_CONTENT);
            Files.deleteIfExists(filePath);
        }

        assertThat(modulesDir.listFiles()).isEmpty();
    }

    @Test
    void whenDiscoveringModels_AndCMHandlesHaveDifferentNumbersOfModules_ThenCorrectYangFilesAreCreated()
            throws CMServiceException, IOException, ModelParserException {
        objUnderTest = new ModelDiscoveryService(mockNcmpService, DUMMY_NCMP_URL, modulesDir.getPath() + '/', modelManagerImpl, modelParser);

        when(mockNcmpService.getCMHandleModuleRevisions(MODULE_NAMES)).thenReturn(createDifferentNumbersOfCmHandleModuleRevisions());
        when(mockNcmpService.getModuleDefinitions(CM_HANDLE_1)).thenReturn(create4RestModuleDefinitions());
        when(mockNcmpService.getModuleDefinitions(CM_HANDLE_2)).thenReturn(create5RestModuleDefinitions());

        final ApplicationReadyEvent applicationReadyEvent = mock(ApplicationReadyEvent.class);
        objUnderTest.runOnStartup(applicationReadyEvent);
        await().atMost(1, SECONDS).until(() -> true);

        verify(mockNcmpService, times(1)).getCMHandleModuleRevisions(MODULE_NAMES);
        verify(mockNcmpService, times(1)).getModuleDefinitions(CM_HANDLE_1);
        verify(mockNcmpService, times(1)).getModuleDefinitions(CM_HANDLE_2);
        verify(modelParser, times(1)).generateModel(anyList(), any());

        assertThat(modulesDir.listFiles()).hasSize(5);

        for (final String expectedFileName : create5ExpectedFileNames()) {
            assertThat(new File(expectedFileName)).exists();
            final Path filePath = Paths.get(expectedFileName);
            final String fileContent = Files.readString(filePath);
            assertThat(fileContent).as("Unexpected content for file:%s", expectedFileName).isEqualTo(FILE_CONTENT);
            Files.deleteIfExists(filePath);
        }

        assertThat(modulesDir.listFiles()).isEmpty();
    }

    @Test
    void whenDiscoveringModels_AndCMHandlesHaveSameModulesWithDifferentVersions_ThenCorrectYangFilesAreCreated()
            throws CMServiceException, IOException, ModelParserException {
        objUnderTest = new ModelDiscoveryService(mockNcmpService, DUMMY_NCMP_URL, modulesDir.getPath() + '/', modelManagerImpl, modelParser);

        when(mockNcmpService.getCMHandleModuleRevisions(MODULE_NAMES)).thenReturn(createCmHandleModuleRevisionsWithDifferentVersions());
        when(mockNcmpService.getModuleDefinitions(CM_HANDLE_1)).thenReturn(create4RestModuleDefinitions());
        when(mockNcmpService.getModuleDefinitions(CM_HANDLE_2)).thenReturn(create4RestModuleDefinitionsWithDifferentVersions());

        final ApplicationReadyEvent applicationReadyEvent = mock(ApplicationReadyEvent.class);
        objUnderTest.runOnStartup(applicationReadyEvent);
        await().atMost(1, SECONDS).until(() -> modulesDir.listFiles().length == 8);

        verify(mockNcmpService, times(1)).getCMHandleModuleRevisions(MODULE_NAMES);
        verify(mockNcmpService, times(1)).getModuleDefinitions(CM_HANDLE_1);
        verify(mockNcmpService, times(1)).getModuleDefinitions(CM_HANDLE_2);
        verify(modelParser, times(1)).generateModel(anyList(), any());

        assertThat(modulesDir.listFiles()).hasSize(8);

        for (final String expectedFileName : create8ExpectedFileNames()) {
            assertThat(new File(expectedFileName).exists()).isTrue();
            final Path filePath = Paths.get(expectedFileName);
            final String fileContent = Files.readString(filePath);
            assertThat(fileContent).as("Unexpected content for file:%s", expectedFileName).isEqualTo(FILE_CONTENT);
            Files.deleteIfExists(filePath);
        }

        assertThat(modulesDir.listFiles().length).isEqualTo(0);
    }

    @Test
    void whenDiscoveringModels_AndExceptionOccursWhileRetrievingModuleDefinitions_ThenModuleRevisionsAreStoredButNoYangModulesDirectoryIsCreatedButEmpty()
            throws CMServiceException, ModelParserException {
        objUnderTest = new ModelDiscoveryService(mockNcmpService, DUMMY_NCMP_URL, modulesDir.getPath() + '/', modelManagerImpl, modelParser);

        when(mockNcmpService.getCMHandleModuleRevisions(MODULE_NAMES)).thenReturn(createIdenticalCmHandleModuleRevisions());
        when(mockNcmpService.getModuleDefinitions(CM_HANDLE_1)).thenThrow(CMServiceException.class);

        final ApplicationReadyEvent applicationReadyEvent = mock(ApplicationReadyEvent.class);
        objUnderTest.runOnStartup(applicationReadyEvent);
        await().atMost(1, SECONDS).until(() -> true);

        verify(mockNcmpService, times(1)).getCMHandleModuleRevisions(MODULE_NAMES);
        verify(mockNcmpService, times(1)).getModuleDefinitions(CM_HANDLE_1);
        verify(mockNcmpService, never()).getModuleDefinitions(CM_HANDLE_2);
        verify(modelParser, never()).generateModel(anyList(), any());

        assertThat(Path.of(modulesDir.getPath())).exists();
        assertThat(modulesDir.listFiles()).isEmpty();
    }

    @Test
    void whenDiscoveringModels_AndNcmpUrlIsNull_ThenNoNcmpCallsAreMade() throws CMServiceException, ModelParserException {
        objUnderTest = new ModelDiscoveryService(mockNcmpService, null, modulesDir.getPath() + '/', modelManagerImpl, modelParser);

        final ApplicationReadyEvent applicationReadyEvent = mock(ApplicationReadyEvent.class);
        objUnderTest.runOnStartup(applicationReadyEvent);

        verify(mockNcmpService, never()).getCMHandleModuleRevisions(MODULE_NAMES);
        verify(mockNcmpService, never()).getModuleDefinitions(CM_HANDLE_1);
        verify(mockNcmpService, never()).getModuleDefinitions(CM_HANDLE_2);
        verify(modelParser, never()).generateModel(anyList(), any());

    }

    private List<CMHandleModuleRevision> createIdenticalCmHandleModuleRevisions() {
        return List.of(
                new CMHandleModuleRevision(CM_HANDLE_1, "ericsson-enm-Lrat", "2110-01-20"),
                new CMHandleModuleRevision(CM_HANDLE_1, "ericsson-enm-GNBCUCP", "2110-02-20"),
                new CMHandleModuleRevision(CM_HANDLE_1, "ericsson-enm-GNBCUUP", "2110-03-20"),
                new CMHandleModuleRevision(CM_HANDLE_1, "ericsson-enm-GNBDU", "2110-04-20"),
                new CMHandleModuleRevision(CM_HANDLE_2, "ericsson-enm-Lrat", "2110-01-20"),
                new CMHandleModuleRevision(CM_HANDLE_2, "ericsson-enm-GNBCUCP", "2110-02-20"),
                new CMHandleModuleRevision(CM_HANDLE_2, "ericsson-enm-GNBCUUP", "2110-03-20"),
                new CMHandleModuleRevision(CM_HANDLE_2, "ericsson-enm-GNBDU", "2110-04-20"));
    }

    private List<CMHandleModuleRevision> createDifferentNumbersOfCmHandleModuleRevisions() {
        return List.of(
                new CMHandleModuleRevision(CM_HANDLE_1, "ericsson-enm-Lrat", "2110-01-20"),
                new CMHandleModuleRevision(CM_HANDLE_1, "ericsson-enm-GNBCUCP", "2110-02-20"),
                new CMHandleModuleRevision(CM_HANDLE_1, "ericsson-enm-GNBCUUP", "2110-03-20"),
                new CMHandleModuleRevision(CM_HANDLE_1, "ericsson-enm-GNBDU", "2110-04-20"),
                new CMHandleModuleRevision(CM_HANDLE_2, "ericsson-enm-Lrat", "2110-01-20"),
                new CMHandleModuleRevision(CM_HANDLE_2, "ericsson-enm-GNBCUCP", "2110-02-20"),
                new CMHandleModuleRevision(CM_HANDLE_2, "ericsson-enm-GNBCUUP", "2110-03-20"),
                new CMHandleModuleRevision(CM_HANDLE_2, "ericsson-enm-GNBDU", "2110-04-20"),
                new CMHandleModuleRevision(CM_HANDLE_2, "ericsson-enm-ComTop", "2110-05-20"));
    }

    private List<CMHandleModuleRevision> createCmHandleModuleRevisionsWithDifferentVersions() {
        return List.of(
                new CMHandleModuleRevision(CM_HANDLE_1, "ericsson-enm-Lrat", "2110-01-20"),
                new CMHandleModuleRevision(CM_HANDLE_1, "ericsson-enm-GNBCUCP", "2110-02-20"),
                new CMHandleModuleRevision(CM_HANDLE_1, "ericsson-enm-GNBCUUP", "2110-03-20"),
                new CMHandleModuleRevision(CM_HANDLE_1, "ericsson-enm-GNBDU", "2110-04-20"),
                new CMHandleModuleRevision(CM_HANDLE_2, "ericsson-enm-Lrat", "2110-01-30"),
                new CMHandleModuleRevision(CM_HANDLE_2, "ericsson-enm-GNBCUCP", "2110-02-30"),
                new CMHandleModuleRevision(CM_HANDLE_2, "ericsson-enm-GNBCUUP", "2110-03-30"),
                new CMHandleModuleRevision(CM_HANDLE_2, "ericsson-enm-GNBDU", "2110-04-30"));
    }

    private List<RestModuleDefinition> create4RestModuleDefinitions() {
        return List.of(
                new RestModuleDefinition("ericsson-enm-Lrat", "2110-01-20", FILE_CONTENT),
                new RestModuleDefinition("ericsson-enm-GNBCUCP", "2110-02-20", FILE_CONTENT),
                new RestModuleDefinition("ericsson-enm-GNBCUUP", "2110-03-20", FILE_CONTENT),
                new RestModuleDefinition("ericsson-enm-GNBDU", "2110-04-20", FILE_CONTENT));
    }

    private List<RestModuleDefinition> create4RestModuleDefinitionsWithDifferentVersions() {
        return List.of(
                new RestModuleDefinition("ericsson-enm-Lrat", "2110-01-30", FILE_CONTENT),
                new RestModuleDefinition("ericsson-enm-GNBCUCP", "2110-02-30", FILE_CONTENT),
                new RestModuleDefinition("ericsson-enm-GNBCUUP", "2110-03-30", FILE_CONTENT),
                new RestModuleDefinition("ericsson-enm-GNBDU", "2110-04-30", FILE_CONTENT));
    }

    private List<RestModuleDefinition> create5RestModuleDefinitions() {
        return List.of(new RestModuleDefinition("ericsson-enm-Lrat", "2110-01-20", FILE_CONTENT),
                new RestModuleDefinition("ericsson-enm-GNBCUCP", "2110-02-20", FILE_CONTENT),
                new RestModuleDefinition("ericsson-enm-GNBCUUP", "2110-03-20", FILE_CONTENT),
                new RestModuleDefinition("ericsson-enm-GNBDU", "2110-04-20", FILE_CONTENT),
                new RestModuleDefinition("ericsson-enm-ComTop", "2110-05-20", FILE_CONTENT));
    }

    private List<String> create4ExpectedFileNames() {
        return List.of(path + "/ericsson-enm-Lrat-2110-01-20.yang",
                path + "/ericsson-enm-GNBCUCP-2110-02-20.yang",
                path + "/ericsson-enm-GNBCUUP-2110-03-20.yang",
                path + "/ericsson-enm-GNBDU-2110-04-20.yang");
    }

    private List<String> create5ExpectedFileNames() {
        return List.of(
                path + "/ericsson-enm-Lrat-2110-01-20.yang",
                path + "/ericsson-enm-GNBCUCP-2110-02-20.yang",
                path + "/ericsson-enm-GNBCUUP-2110-03-20.yang",
                path + "/ericsson-enm-GNBDU-2110-04-20.yang",
                path + "/ericsson-enm-ComTop-2110-05-20.yang");
    }

    private List<String> create8ExpectedFileNames() {
        return List.of(
                path + "/ericsson-enm-Lrat-2110-01-20.yang",
                path + "/ericsson-enm-GNBCUCP-2110-02-20.yang",
                path + "/ericsson-enm-GNBCUUP-2110-03-20.yang",
                path + "/ericsson-enm-GNBDU-2110-04-20.yang",
                path + "/ericsson-enm-Lrat-2110-01-30.yang",
                path + "/ericsson-enm-GNBCUCP-2110-02-30.yang",
                path + "/ericsson-enm-GNBCUUP-2110-03-30.yang",
                path + "/ericsson-enm-GNBDU-2110-04-30.yang");
    }

}
