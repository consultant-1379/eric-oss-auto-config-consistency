/*******************************************************************************
 * COPYRIGHT Ericsson 2024
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

package com.ericsson.oss.apps.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.apps.executor.AuditStatus;
import com.ericsson.oss.apps.executor.ChangeStatus;
import com.ericsson.oss.apps.model.AuditResult;

/**
 * Unit tests for {@link AuditResultsSpecification}.
 */
@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(MockitoExtension.class)
public class AuditResultsSpecificationTest {
    private static final String EXECUTION_ID = "1";
    private static final String EXECUTION_ID_TWO = "2";
    @Autowired
    AuditResultsRepository auditResultsRepository;

    @Test
    void findsAllByExecutionIdWhereSingleEnumValueFilter() {
        final AuditResult auditResult1 = new AuditResult();
        auditResult1.setExecutionId(EXECUTION_ID);
        auditResult1.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult1.setManagedObjectFdn("auditResultFdn1");

        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId(EXECUTION_ID);
        auditResult2.setAuditStatus(AuditStatus.CONSISTENT);
        auditResult1.setManagedObjectFdn("auditResultFdn2");

        auditResultsRepository.saveAllAndFlush(List.of(auditResult1, auditResult2));

        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(FilterCriteria.build("executionId:1"));

        final AuditResultsSpecification auditResultsFilterSpecification = new AuditResultsSpecification(
                FilterCriteria.build("auditStatus:Inconsistent"));

        final Optional<Page<AuditResult>> auditResults = Optional
                .of(auditResultsRepository.findAll(Specification.where(auditResultsExecutionIdSpecification).and(auditResultsFilterSpecification),
                        Pageable.unpaged()));
        assertThat(auditResults.isPresent()).isTrue();
        assertThat(auditResults.get()).hasSize(1)
                .extracting(AuditResult::getManagedObjectFdn, AuditResult::getExecutionId, AuditResult::getAuditStatus)
                .containsExactly(
                        tuple("auditResultFdn2", EXECUTION_ID, AuditStatus.INCONSISTENT));

    }

    @Test
    void findsAllByExecutionIdWhereMultiEnumValueFilter() {
        final AuditResult auditResult1 = new AuditResult();
        auditResult1.setExecutionId(EXECUTION_ID);
        auditResult1.setChangeStatus(ChangeStatus.IMPLEMENTATION_COMPLETE);
        auditResult1.setManagedObjectFdn("auditResultFdn1");

        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId(EXECUTION_ID);
        auditResult2.setChangeStatus(ChangeStatus.IMPLEMENTATION_IN_PROGRESS);
        auditResult2.setManagedObjectFdn("auditResultFdn2");

        final AuditResult auditResult3 = new AuditResult();
        auditResult3.setExecutionId(EXECUTION_ID);
        auditResult3.setChangeStatus(null);
        auditResult3.setManagedObjectFdn("auditResultFdn3");

        final AuditResult auditResult4 = new AuditResult();
        auditResult4.setExecutionId(EXECUTION_ID);
        auditResult4.setChangeStatus(ChangeStatus.IMPLEMENTATION_FAILED);
        auditResult4.setManagedObjectFdn("auditResultFdn4");

        final AuditResult auditResult5 = new AuditResult();
        auditResult5.setExecutionId(EXECUTION_ID_TWO);
        auditResult5.setChangeStatus(ChangeStatus.IMPLEMENTATION_COMPLETE);
        auditResult5.setManagedObjectFdn("auditResultFdn5");

        auditResultsRepository.saveAllAndFlush(List.of(auditResult1, auditResult2, auditResult3, auditResult4, auditResult5));

        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(FilterCriteria.build("executionId:1"));

        final AuditResultsSpecification auditResultsFilterSpecification = new AuditResultsSpecification(
                FilterCriteria.build("changeStatus:(Implementation complete,Implementation in Progress,Implementation Failed)"));

        final Optional<Page<AuditResult>> auditResults = Optional
                .of(auditResultsRepository.findAll(Specification.where(auditResultsExecutionIdSpecification).and(auditResultsFilterSpecification),
                        Pageable.unpaged()));
        assertThat(auditResults.isPresent()).isTrue();
        assertThat(auditResults.get()).hasSize(3)
                .extracting(AuditResult::getManagedObjectFdn, AuditResult::getExecutionId, AuditResult::getChangeStatus)
                .containsExactlyInAnyOrder(
                        tuple("auditResultFdn1", EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE),
                        tuple("auditResultFdn2", EXECUTION_ID, ChangeStatus.IMPLEMENTATION_IN_PROGRESS),
                        tuple("auditResultFdn4", EXECUTION_ID, ChangeStatus.IMPLEMENTATION_FAILED));

        //Mimic no filter scenario
        final Optional<Page<AuditResult>> auditResultsNoFilter = Optional
                .of(auditResultsRepository.findAll(Specification.where(auditResultsExecutionIdSpecification).and(null),
                        Pageable.unpaged()));

        assertThat(auditResultsNoFilter.isPresent()).isTrue();
        assertThat(auditResultsNoFilter.get()).hasSize(4)
                .extracting(AuditResult::getManagedObjectFdn, AuditResult::getExecutionId, AuditResult::getChangeStatus)
                .containsExactlyInAnyOrder(
                        tuple("auditResultFdn1", EXECUTION_ID, ChangeStatus.IMPLEMENTATION_COMPLETE),
                        tuple("auditResultFdn2", EXECUTION_ID, ChangeStatus.IMPLEMENTATION_IN_PROGRESS),
                        tuple("auditResultFdn3", EXECUTION_ID, null),
                        tuple("auditResultFdn4", EXECUTION_ID, ChangeStatus.IMPLEMENTATION_FAILED));
    }

    @Test
    void findsAllByExecutionIdWhereSingleEnumValueNotFilter() {
        final AuditResult auditResult1 = new AuditResult();
        auditResult1.setExecutionId(EXECUTION_ID);
        auditResult1.setAuditStatus(AuditStatus.INCONSISTENT);
        auditResult1.setManagedObjectFdn("auditResultFdn1");
        auditResult1.setChangeStatus(ChangeStatus.NOT_APPLIED);

        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId(EXECUTION_ID);
        auditResult2.setAuditStatus(AuditStatus.CONSISTENT);
        auditResult1.setManagedObjectFdn("auditResultFdn2");
        auditResult2.setChangeStatus(ChangeStatus.IMPLEMENTATION_COMPLETE);

        auditResultsRepository.saveAllAndFlush(List.of(auditResult1, auditResult2));

        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(FilterCriteria.build("executionId:1"));

        final AuditResultsSpecification auditResultsAuditStatusSpecification = new AuditResultsSpecification(
                FilterCriteria.build("auditStatus:Inconsistent"));

        final AuditResultsSpecification auditResultsChangeStatusNotSpecification = new AuditResultsSpecification(
                FilterCriteria.build("changeStatus:!" +ChangeStatus.IMPLEMENTATION_COMPLETE));

        final Optional<Page<AuditResult>> auditResults = Optional
                .of(auditResultsRepository.findAll(Specification.where(auditResultsExecutionIdSpecification).and(auditResultsAuditStatusSpecification).and(auditResultsChangeStatusNotSpecification),
                        Pageable.unpaged()));
        assertThat(auditResults).isPresent();
        assertThat(auditResults.get()).hasSize(1)
                .extracting(AuditResult::getManagedObjectFdn, AuditResult::getExecutionId, AuditResult::getAuditStatus,
                        AuditResult::getChangeStatus)
                .containsExactly(
                        tuple("auditResultFdn2", EXECUTION_ID, AuditStatus.INCONSISTENT,
                                ChangeStatus.NOT_APPLIED));
    }

    @Test
    void findsAllManagedObjectFdnByExecutionIdWhereSingleFilterValueInCaseInsensitiveFormatAndPercenatgeOnEitherSide() {
        final AuditResult auditResult1 = new AuditResult();
        auditResult1.setExecutionId(EXECUTION_ID);
        auditResult1.setManagedObjectFdn("auditResultFdn1");

        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId(EXECUTION_ID);
        auditResult2.setManagedObjectFdn("auditResultFdn2");

        auditResultsRepository.saveAllAndFlush(List.of(auditResult1, auditResult2));

        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(FilterCriteria.build("executionId:1"));

        final AuditResultsSpecification auditResultsFilterSpecification = new AuditResultsSpecification(
                FilterCriteria.build("managedObjectFdn:%fdn2%"));

        final Optional<Page<AuditResult>> auditResults = Optional
                .of(auditResultsRepository.findAll(Specification.where(auditResultsExecutionIdSpecification).and(auditResultsFilterSpecification),
                        Pageable.unpaged()));
        assertThat(auditResults.isPresent()).isTrue();
        assertThat(auditResults.get()).hasSize(1)
                .extracting(AuditResult::getManagedObjectFdn, AuditResult::getExecutionId)
                .containsExactly(
                        tuple("auditResultFdn2", EXECUTION_ID));

    }

    @Test
    void findsAllManagedObjectFdnByExecutionIdWhereSingleFilterValueAndPercenatgeOnRightSide() {
        final AuditResult auditResult1 = new AuditResult();
        auditResult1.setExecutionId(EXECUTION_ID);
        auditResult1.setManagedObjectFdn("auditRe%sultFdn1");

        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId(EXECUTION_ID);
        auditResult2.setManagedObjectFdn("auditRe%sultFdn2");

        final AuditResult auditResult3 = new AuditResult();
        auditResult3.setExecutionId(EXECUTION_ID_TWO);
        auditResult3.setManagedObjectFdn("auditResultFdn3");

        auditResultsRepository.saveAllAndFlush(List.of(auditResult1, auditResult2, auditResult3));

        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(FilterCriteria.build("executionId:1"));

        final AuditResultsSpecification auditResultsFilterSpecification = new AuditResultsSpecification(
                FilterCriteria.build("managedObjectFdn:auditRe%%"));

        final Optional<Page<AuditResult>> auditResults = Optional
                .of(auditResultsRepository.findAll(Specification.where(auditResultsExecutionIdSpecification).and(auditResultsFilterSpecification),
                        Pageable.unpaged()));
        assertThat(auditResults.isPresent()).isTrue();
        assertThat(auditResults.get()).hasSize(2)
                .extracting(AuditResult::getManagedObjectFdn, AuditResult::getExecutionId)
                .containsExactly(
                        tuple("auditRe%sultFdn1", EXECUTION_ID), tuple("auditRe%sultFdn2", EXECUTION_ID));

    }

    @Test
    void findsAllManagedObjectFdnByExecutionIdWhereSingleFilterValueAndPercenatgeOnLeftSide() {
        final AuditResult auditResult1 = new AuditResult();
        auditResult1.setExecutionId(EXECUTION_ID);
        auditResult1.setManagedObjectFdn("auditResultFdn1");

        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId(EXECUTION_ID);
        auditResult2.setManagedObjectFdn("auditResultFdn2");

        final AuditResult auditResult3 = new AuditResult();
        auditResult3.setExecutionId(EXECUTION_ID_TWO);
        auditResult3.setManagedObjectFdn("auditResultFdn3");

        auditResultsRepository.saveAllAndFlush(List.of(auditResult1, auditResult2, auditResult3));

        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(FilterCriteria.build("executionId:1"));

        final AuditResultsSpecification auditResultsFilterSpecification = new AuditResultsSpecification(
                FilterCriteria.build("managedObjectFdn:%1"));

        final Optional<Page<AuditResult>> auditResults = Optional
                .of(auditResultsRepository.findAll(Specification.where(auditResultsExecutionIdSpecification).and(auditResultsFilterSpecification),
                        Pageable.unpaged()));
        assertThat(auditResults.isPresent()).isTrue();
        assertThat(auditResults.get()).hasSize(1)
                .extracting(AuditResult::getManagedObjectFdn, AuditResult::getExecutionId)
                .containsExactly(
                        tuple("auditResultFdn1", EXECUTION_ID));

    }

    @Test
    void findsAllManagedObjectFdnByExecutionIdWhereSingleFilterValueAndNoPercenatgeOnEitherSideAndHasOneExactMatch() {
        final AuditResult auditResult1 = new AuditResult();
        auditResult1.setExecutionId(EXECUTION_ID);
        auditResult1.setManagedObjectFdn("auditResultFdn1");

        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId(EXECUTION_ID);
        auditResult2.setManagedObjectFdn("auditResultFdn2");

        final AuditResult auditResult3 = new AuditResult();
        auditResult3.setExecutionId(EXECUTION_ID_TWO);
        auditResult3.setManagedObjectFdn("auditResultFdn3");

        auditResultsRepository.saveAllAndFlush(List.of(auditResult1, auditResult2, auditResult3));

        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(FilterCriteria.build("executionId:2"));

        final AuditResultsSpecification auditResultsFilterSpecification = new AuditResultsSpecification(
                FilterCriteria.build("managedObjectFdn:auditResultFdn3"));

        final Optional<Page<AuditResult>> auditResults = Optional
                .of(auditResultsRepository.findAll(Specification.where(auditResultsExecutionIdSpecification).and(auditResultsFilterSpecification),
                        Pageable.unpaged()));
        assertThat(auditResults.isPresent()).isTrue();
        assertThat(auditResults.get()).hasSize(1)
                .extracting(AuditResult::getManagedObjectFdn, AuditResult::getExecutionId)
                .containsExactly(
                        tuple("auditResultFdn3", EXECUTION_ID_TWO));

    }

    @Test
    void findsAllManagedObjectFdnByExecutionIdWhereSingleFilterValueAndNoPercenatgeOnEitherSideAndNoResultExpected() {
        final AuditResult auditResult1 = new AuditResult();
        auditResult1.setExecutionId(EXECUTION_ID);
        auditResult1.setManagedObjectFdn("auditResultFdn1");

        final AuditResult auditResult2 = new AuditResult();
        auditResult2.setExecutionId(EXECUTION_ID);
        auditResult2.setManagedObjectFdn("auditResultFdn2");

        final AuditResult auditResult3 = new AuditResult();
        auditResult3.setExecutionId(EXECUTION_ID_TWO);
        auditResult3.setManagedObjectFdn("auditResultFdn3");

        auditResultsRepository.saveAllAndFlush(List.of(auditResult1, auditResult2, auditResult3));

        final AuditResultsSpecification auditResultsExecutionIdSpecification = new AuditResultsSpecification(FilterCriteria.build("executionId:1"));

        final AuditResultsSpecification auditResultsFilterSpecification = new AuditResultsSpecification(
                FilterCriteria.build("managedObjectFdn:nofdnmatch"));

        final Optional<Page<AuditResult>> auditResults = Optional
                .of(auditResultsRepository.findAll(Specification.where(auditResultsExecutionIdSpecification).and(auditResultsFilterSpecification),
                        Pageable.unpaged()));
        assertThat(auditResults.isPresent()).isTrue();
        assertThat(auditResults.get()).hasSize(0);

    }

}