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

import static com.ericsson.oss.apps.util.Constants.NODE_FDN_MAX_LENGTH;
import static com.ericsson.oss.apps.util.Constants.SCOPES_URI;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.apps.api.model.EaccScopeMetadata;
import com.ericsson.oss.apps.model.Scope;
import com.ericsson.oss.apps.repository.ScopeRepository;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.opencsv.exceptions.CsvValidationException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScopeService {
    private static final String DUMMY_ID = "298949fe-d222-4191-a192-3eccb9f74cda";
    private static final String DUMMY_NAME = "ScopeName";
    private static final String URI_BASE = "v1/scopes/";
    private static final String[] CSV_HEADER = { "fdn" };

    private final ScopeRepository scopeRepository;

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public List<EaccScopeMetadata> getAllScopeMetadata() {
        return scopeRepository.findAll().stream()
                .map(this::convertToMetaData)
                .collect(Collectors.toList());
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public EaccScopeMetadata updateScope(final Scope scope, final MultipartFile file) throws CsvRequiredFieldEmptyException,
            CsvValidationException, IOException {
        final List<String> fdns = parseCsvToFdns(file);
        scope.setFdns(fdns);
        log.debug("Updating scope in the database.");
        scopeRepository.save(scope);
        return createEaccScopeMetadata(scope);
    }

    public EaccScopeMetadata createEaccScopeMetadata(final Scope scope) {
        final EaccScopeMetadata eaccScopeMetadata = new EaccScopeMetadata();
        eaccScopeMetadata.setId(scope.getId().toString());
        eaccScopeMetadata.setScopeName(scope.getName());
        eaccScopeMetadata.setUri(URI_BASE + scope.getId());
        return eaccScopeMetadata;
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public void deleteScope(final UUID id) {
        final Optional<Scope> optionalScope = scopeRepository.findById(id);
        if (optionalScope.isPresent()) {
            scopeRepository.deleteById(id);
        } else {
            log.error("Tried to delete Scope that does not exist.");
            throw new IllegalArgumentException();
        }
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public Scope getScopeFromUUID(final UUID scopeUUID) {
        final Optional<Scope> optionalScope = scopeRepository.findById(scopeUUID);
        if (optionalScope.isPresent()) {
            return optionalScope.get();
        } else {
            log.error("Tried to get Scope for ID that does not exist.");
            throw new IllegalArgumentException();
        }
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public ByteArrayResource getCsvForScope(final UUID uuid) throws IOException {
        final Scope scope = getScopeFromUUID(uuid);
        try (final StringWriter stringWriter = new StringWriter();
                 final ICSVWriter csvWriter = new CSVWriterBuilder(stringWriter)
                         .withEscapeChar('\\')
                         .withLineEnd(ICSVWriter.DEFAULT_LINE_END)
                         .build()) {
                csvWriter.writeNext(CSV_HEADER, false);
                for (final String fdn : scope.getFdns()) {
                    csvWriter.writeNext(new String[] { fdn });
                }
                return new ByteArrayResource(stringWriter.toString().getBytes(StandardCharsets.UTF_8));
            }
        }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public Scope createScopeFromCsv(final String scopeName, final MultipartFile file) throws CsvRequiredFieldEmptyException,
            CsvValidationException, IOException, DataAccessException {
        final List<String> fdns = parseCsvToFdns(file);
        final Scope scope = new Scope();
        scope.setName(scopeName);
        scope.setFdns(fdns);
        log.debug("Saving scope with name: {} to the database.", scopeName);
        scopeRepository.save(scope);
        return scope;
    }

    public EaccScopeMetadata convertToMetaData(final Scope scope) {
        final EaccScopeMetadata eaccScopeMetadata = new EaccScopeMetadata();
        eaccScopeMetadata.setId(String.valueOf(scope.getId()));
        eaccScopeMetadata.setScopeName(scope.getName());
        eaccScopeMetadata.setUri(SCOPES_URI + scope.getId());
        return eaccScopeMetadata;
    }

    @Retryable(retryFor = {
            DataAccessException.class, TransactionException.class }, maxAttemptsExpression = "${database.retry.userInitiated.maxAttempts}",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 3))
    public boolean existsByName(final String scopeName) throws DataAccessException {
        return scopeRepository.existsByName(scopeName);
    }

    public List<String> parseCsvToFdns(final MultipartFile file) throws CsvRequiredFieldEmptyException, CsvValidationException, IOException {
        final List<String> fdnList = new ArrayList<>();
        try (final Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            final CsvToBean<FdnEntry> csvToBean = new CsvToBeanBuilder<FdnEntry>(reader)
                    .withType(FdnEntry.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withFieldAsNull(CSVReaderNullFieldIndicator.NEITHER)
                    .withIgnoreEmptyLine(true)
                    .build();
            try {
                for (final FdnEntry fdnEntry : csvToBean) {
                    validateFdn(fdnEntry.getFdn());
                    fdnList.add(fdnEntry.getFdn());
                }
                //CHECKSTYLE.OFF: IllegalCatch
            } catch (final RuntimeException e) {
                log.error("File column headers/values must match the csv file format.", e);
                throw new CsvRequiredFieldEmptyException("File column headers/values must match the csv file format."); // NOPMD
                //CHECKSTYLE.ON: IllegalCatch
            }
        }
        return fdnList;
    }

    private void validateFdn(final String fdn) throws CsvValidationException {
        if (fdn.length() > NODE_FDN_MAX_LENGTH) {
            throw new CsvValidationException("FDN value is too long at more than " + NODE_FDN_MAX_LENGTH + " characters: " + fdn);
        }
    }

    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FdnEntry {
        @CsvBindByName(required = true)
        private String fdn;
    }
}
