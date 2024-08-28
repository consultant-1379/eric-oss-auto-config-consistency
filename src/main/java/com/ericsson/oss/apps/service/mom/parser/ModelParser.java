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

package com.ericsson.oss.apps.service.mom.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.ericsson.oss.apps.service.mom.model.Model;
import com.ericsson.oss.apps.service.mom.model.ModelObject;
import com.ericsson.oss.apps.service.mom.model.Models;
import com.ericsson.oss.apps.service.mom.parser.attributehandlers.LeafAttributeHandler;
import com.ericsson.oss.apps.service.ncmp.model.CMHandleModuleRevision;
import com.ericsson.oss.mediation.modeling.yangtools.parser.FileBasedYangInput;
import com.ericsson.oss.mediation.modeling.yangtools.parser.ParserExecutionContext;
import com.ericsson.oss.mediation.modeling.yangtools.parser.YangDeviceModel;
import com.ericsson.oss.mediation.modeling.yangtools.parser.findings.FindingsManager;
import com.ericsson.oss.mediation.modeling.yangtools.parser.findings.ModifyableFindingSeverityCalculator;
import com.ericsson.oss.mediation.modeling.yangtools.parser.model.ConformanceType;
import com.ericsson.oss.mediation.modeling.yangtools.parser.model.YangModelInput;
import com.ericsson.oss.mediation.modeling.yangtools.parser.model.statements.AbstractStatement;
import com.ericsson.oss.mediation.modeling.yangtools.parser.model.statements.StatementClassSupplier;
import com.ericsson.oss.mediation.modeling.yangtools.parser.model.statements.eri.EriCustomProcessor;
import com.ericsson.oss.mediation.modeling.yangtools.parser.model.statements.eri.EricssonExtensionsClassSupplier;
import com.ericsson.oss.mediation.modeling.yangtools.parser.model.statements.yang.YGrouping;

import lombok.extern.slf4j.Slf4j;

/**
 * Class to parse the yang files retrieved by the {@link com.ericsson.oss.apps.service.ModelDiscoveryService} and create the eacc minified mom
 * {@link Models} from the yang files.
 */
@Slf4j
@Service
public class ModelParser {

    /**
     * Generates the eacc minified mom {@link Models} for the given module revisions from the yang files in the given directory.
     *
     * @param cmHandleModuleRevisions
     *            the <code>Set</code> of {@link CMHandleModuleRevision}(s) containing the unique module names and version to generate the minified
     *            mom for
     * @param dirPath
     *            the directory path of where the yang files are stored
     * @return a {@link Models} holder which represents the eacc minified mom
     * @throws ModelParserException
     *             thrown if any problems occur generating the eacc minified mom
     */
    public Models generateModel(final List<CMHandleModuleRevision> cmHandleModuleRevisions, final File dirPath) throws ModelParserException {
        log.info("Generation of models started");
        final Models models;
        try {
            models = new Models();
            final YangDeviceModel yangDeviceModel = getYangDeviceModel(dirPath);
            //Create the handlers. Only leaf is supported for now as can only define leaf type attributes in the ruleset
            final LeafAttributeHandler leafHandler = new LeafAttributeHandler();

            for (final CMHandleModuleRevision cmHandleModuleRevision : cmHandleModuleRevisions) {
                final YangModelInput module = yangDeviceModel.getModuleRegistry().find(cmHandleModuleRevision.getModuleName(),
                        cmHandleModuleRevision.getModuleRevision());
                if (module == null) {
                    log.error("Module Revision does not exist {} {}", cmHandleModuleRevision.getModuleName(),
                            cmHandleModuleRevision.getModuleRevision());
                    throw new ModelParserException(String.format("Module Revision does not exist %s %s", cmHandleModuleRevision.getModuleName(),
                            cmHandleModuleRevision.getModuleRevision()));
                }
                final Model model = new Model(module.getModuleIdentity().getModuleName(), module.getModuleIdentity().getRevision());

                final AbstractStatement statement = module.getYangModelRoot().getModuleOrSubmodule();
                final List<YGrouping> groupings = statement.getYangModelRoot().getModule().getGroupings();
                for (final YGrouping grouping : groupings) {
                    //Don't want the keys groupings. Also remove struct- groupings as not supporting for now
                    if (grouping.getGroupingName().endsWith("-keys") || grouping.getGroupingName().startsWith("struct-")) {
                        continue;
                    }

                    final ModelObject modelObject = new ModelObject(grouping.getGroupingName(), leafHandler.handle(grouping.getLeafs()));

                    model.getModelObjectsMapKeyedByName().put(modelObject.getObjectType(), modelObject);
                }
                models.getModelsMapKeyedByModuleNameAndRevision().put(model.getModuleName() + ":" + model.getModuleVersion(), model);
            }
        } catch (final Exception e) {
            log.error("Failed to generate model", e);
            throw new ModelParserException("Failed to generate models", e);
        }
        log.info("Generation of models finished successfully");
        return models;

    }

    private static YangDeviceModel getYangDeviceModel(final File yangFileDir) {
        final YangDeviceModel yangDeviceModel = new YangDeviceModel("eacc");
        final List<YangModelInput> yangModelInputs = new ArrayList<>();
        for (final File file : Objects.requireNonNull(yangFileDir.listFiles())) {
            if (file.isFile() && file.getName().endsWith(".yang")) {
                yangModelInputs.add(new YangModelInput(new FileBasedYangInput(file), ConformanceType.IMPORT));
            }
        }

        final ModifyableFindingSeverityCalculator severityCalculator = new ModifyableFindingSeverityCalculator();
        final FindingsManager findingsManager = new FindingsManager(severityCalculator);
        findingsManager.addFineGrainedFilterString("ietf*,iana*;*;*");

        final StatementClassSupplier eriStatementFactory = new EricssonExtensionsClassSupplier();
        final EriCustomProcessor eriCustomProcessor = new EriCustomProcessor();

        final ParserExecutionContext context = new ParserExecutionContext(findingsManager, List.of(eriStatementFactory),
                List.of(eriCustomProcessor));
        context.setIgnoreImportedProtocolAccessibleObjects(true);
        context.setFailFast(false);

        yangDeviceModel.parseIntoYangModels(context, yangModelInputs);

        return yangDeviceModel;
    }
}
