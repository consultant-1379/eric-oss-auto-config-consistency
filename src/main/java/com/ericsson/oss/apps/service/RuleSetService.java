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

package com.ericsson.oss.apps.service;

import static com.ericsson.oss.apps.util.Constants.FAILED_TO_CREATE_RULESET;
import static com.ericsson.oss.apps.util.Constants.MAX_RULES;
import static com.ericsson.oss.apps.util.Constants.MAX_RULES_EXCEEDED;
import static com.ericsson.oss.apps.util.Constants.RULESETS_URI;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.apps.api.model.EaccRulesetMetadata;
import com.ericsson.oss.apps.controller.ControllerDetailException;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.model.RuleSet;
import com.ericsson.oss.apps.repository.RuleSetRepository;
import com.ericsson.oss.apps.util.CsvLineNumberReader;
import com.ericsson.oss.apps.validation.RuleSetValidator;
import com.ericsson.oss.apps.validation.RuleValidationException;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleSetService {
    private static final String[] CSV_HEADER = { "moType", "attributeName", "attributeValue", "conditions", "priority" };
    private static final Pattern CSV_PARSING_ERROR_TO_BE_SANITIZED = Pattern.compile("Error parsing CSV line: (\\d+)\\..*");

    private final RuleSetRepository ruleSetRepository;
    private final RuleService ruleService;
    private final RuleSetValidator ruleSetValidator;

    @Retryable(retryFor = { DataAccessException.class, TransactionException.class }, //
            maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", //
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public RuleSet createRulesetFromCsv(final String ruleSetName, final MultipartFile file)
            throws CsvRequiredFieldEmptyException, IOException, RuleValidationException {
        final List<Rule> rules = parseCsvToRules(file);
        ruleSetValidator.validateRules(rules);
        final RuleSet ruleSet = new RuleSet(ruleSetName);
        ruleSet.setRules(rules);
        log.debug("Saving ruleset with name: {} to the database.", ruleSetName);
        ruleSetRepository.save(ruleSet);
        return ruleSet;
    }

    @Retryable(retryFor = { DataAccessException.class, TransactionException.class }, //
            maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", //
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public List<EaccRulesetMetadata> getAllRulesetMetadata() {
        return ruleSetRepository.findAll().stream().map(this::createEaccRulesetMetadata).collect(Collectors.toList());
    }

    @Retryable(retryFor = { DataAccessException.class, TransactionException.class }, //
            maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", // 
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public void deleteRulesetById(final UUID id) {
        final Optional<RuleSet> optionalRuleset = ruleSetRepository.findById(id);
        if (optionalRuleset.isPresent()) {
            ruleSetRepository.deleteById(id);
        } else {
            log.error("Tried to delete Ruleset that does not exist.");
            throw new IllegalArgumentException();
        }
    }

    @Retryable(retryFor = { DataAccessException.class, TransactionException.class }, //
            maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", //
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public String getRulesetNameFromId(final UUID id) {
        final Optional<RuleSet> optionalRuleset = ruleSetRepository.findById(id);
        if (optionalRuleset.isPresent()) {
            return optionalRuleset.get().getName();
        } else {
            log.error("Tried to get name for Ruleset that does not exist.");
            throw new IllegalArgumentException();
        }
    }

    public EaccRulesetMetadata createEaccRulesetMetadata(final RuleSet ruleSet) {
        return new EaccRulesetMetadata(String.valueOf(ruleSet.getId()), ruleSet.getName(), RULESETS_URI + ruleSet.getId());
    }

    @Retryable(retryFor = { DataAccessException.class, TransactionException.class }, //
            maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", //
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public boolean existsByName(final String ruleSetName) {
        return ruleSetRepository.existsByName(ruleSetName);
    }

    @Retryable(retryFor = { DataAccessException.class, TransactionException.class }, //
            maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", //
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public boolean existsById(final UUID uuid) {
        return ruleSetRepository.existsById(uuid);
    }

    public List<Rule> parseCsvToRules(final MultipartFile file) throws CsvRequiredFieldEmptyException, IOException {
        final List<Rule> rules;
        try (final Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            final CsvToBean<Rule> csvToBean = new CsvToBeanBuilder<Rule>(new CsvLineNumberReader(reader))
                    .withType(Rule.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withFieldAsNull(CSVReaderNullFieldIndicator.BOTH)
                    .build();
            try {
                rules = csvToBean.parse();
                //CHECKSTYLE.OFF: IllegalCatch
            } catch (final RuntimeException e) {
                final RuntimeException sanitized = sanitize(e);
                log.error("File has null values or column headers do not match the Rule model", sanitized);
                throw new CsvRequiredFieldEmptyException("File cannot contain null values and column headers must match the Rule model"); // NOPMD
                //CHECKSTYLE.ON: IllegalCatch
            }
        }
        if (rules.size() > MAX_RULES) {
            log.error("More than %d rules is not supported.".formatted(MAX_RULES));
            throw new ControllerDetailException(HttpStatus.BAD_REQUEST, FAILED_TO_CREATE_RULESET, MAX_RULES_EXCEEDED);
        }
        return rules;
    }

    private RuntimeException sanitize(final RuntimeException e) {
        final var matcher = CSV_PARSING_ERROR_TO_BE_SANITIZED.matcher(e.getMessage());
        if (matcher.matches()) {
            return new RuntimeException("Error parsing CSV line: %s".formatted(matcher.group(1)));
        }
        return e;
    }

    public ByteArrayResource createCsvFromRuleset(final UUID id) throws IOException {
        final List<Rule> rules = ruleService.getRulesForRulesetId(id);
        try (final StringWriter stringWriter = new StringWriter();
                final ICSVWriter csvWriter = new CSVWriterBuilder(stringWriter)
                        .withSeparator(',')
                        .withEscapeChar('\\')
                        .withLineEnd(ICSVWriter.DEFAULT_LINE_END)
                        .build()) {
            csvWriter.writeNext(CSV_HEADER);

            for (final Rule rule : rules) {
                csvWriter.writeNext(new String[] { rule.getMoType(), rule.getAttributeName(), rule.getAttributeValue(),
                        rule.getConditions(), rule.getPriority() == null ? "" : String.valueOf(rule.getPriority()) });
            }

            return new ByteArrayResource(stringWriter.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    @Retryable(retryFor = { DataAccessException.class, TransactionException.class }, //
            maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}", //
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public RuleSet updateRulesetFromCsv(final UUID ruleSetId, final MultipartFile file)
            throws CsvRequiredFieldEmptyException, IOException, RuleValidationException {
        final RuleSet ruleSet = ruleSetRepository.getById(ruleSetId);

        final List<Rule> rules = parseCsvToRules(file);
        ruleSetValidator.validateRules(rules);
        ruleSet.setRules(rules);

        log.debug("Updating ruleset in the database.");
        ruleSetRepository.save(ruleSet);

        return ruleSet;
    }
}