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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.client.ncmp.model.RestModuleDefinition;
import com.ericsson.oss.apps.service.mom.parser.ModelParser;
import com.ericsson.oss.apps.service.mom.parser.ModelParserException;
import com.ericsson.oss.apps.service.ncmp.model.CMHandleModuleRevision;
import com.ericsson.oss.apps.validation.ModelManagerImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * A service class to support the building of the MoM at a network level.
 */
@Service
@Slf4j
public class ModelDiscoveryService {

    private final CMService ncmpService;

    private final ModelManagerImpl modelManager;

    private final String ncmpUrl;

    private final String moduleDefinitionsPath;

    private final ModelParser modelParser;

    @Autowired
    public ModelDiscoveryService(final CMService ncmpService, @Value("${gateway.services.ncmp.url}") final String ncmpUrl,
                                 @Value("${modelDiscovery.modulesFilePath}") final String moduleDefinitionsPath,
                                 final ModelManagerImpl modelManager, final ModelParser modelParser) {
        this.ncmpService = ncmpService;
        this.ncmpUrl = ncmpUrl;
        this.moduleDefinitionsPath = moduleDefinitionsPath;
        this.modelManager = modelManager;
        this.modelParser = modelParser;
    }

    private List<CMHandleModuleRevision> calcUniqueModuleRevisions() throws CMServiceException {
        log.info("Retrieving unique module revisions.");
        final List<String> requestedModuleNames =
                List.of("ericsson-enm-Lrat", "ericsson-enm-GNBDU", "ericsson-enm-GNBCUCP", "ericsson-enm-GNBCUUP");
        final List<CMHandleModuleRevision> cmHandleModuleRevisions = ncmpService.getCMHandleModuleRevisions(requestedModuleNames);
        modelManager.setCmHandleModuleRevisionList(cmHandleModuleRevisions);
        return new ArrayList<>(cmHandleModuleRevisions.parallelStream()
                .collect(Collectors.toMap(
                        cmHandleModuleRevision ->
                                cmHandleModuleRevision.getModuleName() + "_" + cmHandleModuleRevision.getModuleRevision(),
                        Function.identity(), (existing, duplicate) -> existing)).values());
    }

    private void readAndWriteModuleDefinitions(final Set<String> cmHandles,
                                               final String moduleDefinitionsPath) throws CMServiceException, IOException {
        log.info("Reading module definitions.");
        final Path modulesDir = Paths.get(moduleDefinitionsPath);
        if (Files.notExists(modulesDir)) {
            Files.createDirectories(modulesDir);
        }
        for (final File file: modulesDir.toFile().listFiles()) {
            Files.delete(Path.of(file.getPath()));
        }
        for (final String cmHandle : cmHandles) {
            final List<RestModuleDefinition> moduleDefinitions = ncmpService.getModuleDefinitions(cmHandle);
            for (final RestModuleDefinition moduleDefinition : moduleDefinitions) {
                final String fileName = moduleDefinition.getModuleName() + "-" + moduleDefinition.getRevision() + ".yang";
                final File module = new File(moduleDefinitionsPath + fileName);
                if (module.exists()) {
                    log.debug("YANG file {} already exists.", fileName);
                } else {
                    log.debug("Creating YANG file {}.", fileName);
                    try (final BufferedWriter fileWriter = Files.newBufferedWriter(
                            Paths.get(moduleDefinitionsPath + fileName), StandardCharsets.UTF_8)) {
                        fileWriter.write(moduleDefinition.getContent());
                        log.debug("YANG file {} successfully created.", fileName);
                    } catch (IOException e) {
                        log.error("Error occurred while creating YANG file " + fileName);
                        throw e;
                    }
                }
            }
        }
        log.info("Module definitions successfully read and written to {}", moduleDefinitionsPath);
    }

    @Scheduled(cron = "${modelDiscovery.scheduling.cronExpression}")
    void discoverNeModels() throws CMServiceException, IOException, ModelParserException {
        final List<CMHandleModuleRevision> uniqueCmHandleModuleRevisions = calcUniqueModuleRevisions();
        final Set<String> cmHandlesWithUniqueModules = uniqueCmHandleModuleRevisions.stream()
                .map(CMHandleModuleRevision::getCMHandle).collect(Collectors.toSet());
        log.info("Found {} unique cmHandles for the unique module name & revision key.", cmHandlesWithUniqueModules.size());

        readAndWriteModuleDefinitions(cmHandlesWithUniqueModules, moduleDefinitionsPath);

        modelManager.setModels(modelParser.generateModel(uniqueCmHandleModuleRevisions, new File(moduleDefinitionsPath)));
    }

    @Async
    @EventListener
    public void runOnStartup(final ApplicationReadyEvent event) {
        if (ncmpUrl == null || ncmpUrl.isEmpty()) {
            log.error("ncmpUrl not set. Not continuing with network models discovery.");
        } else {
            log.info("Executing discoverNeModels on startup...");
            try {
                discoverNeModels();
                log.info("Network Models discovery was successful.");
                modelManager.setModelValidationReady(Boolean.TRUE);
            } catch (final Exception e) {
                log.error("Network models discovery failed", e);
            }
        }
    }

}
