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

package com.ericsson.oss.apps.repository;

import static com.ericsson.assertions.EaccAssertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.ericsson.oss.apps.executor.ChangeStatus;

/**
 * Unit tests for {@link FilterCriteria} interface.
 */
class FilterCriteriaTest {

    @Test
    void buildsFilterCriteriaWithEqOperationWhenSingleValue() {

        final Optional<FilterCriteria> filter = FilterCriteria.build("auditStatus:Inconsistent");
        assertThat(filter).isPresent();
        assertThat(filter.get().getKey()).isEqualTo("auditStatus");
        assertThat(filter.get().getOperation()).isEqualTo(QueryOperator.EQUALS);
        assertThat(filter.get().getValue()).isEqualTo("Inconsistent");
        assertThat(filter.get().getValues()).isNull();

    }

    @Test
    void buildsFilterCriteriaWithNotEqOperationWhenSingleValue() {
        final Optional<FilterCriteria> filter = FilterCriteria.build("changeStatus:!" + ChangeStatus.IMPLEMENTATION_COMPLETE);
        assertThat(filter).isPresent();
        assertThat(filter.get().getKey()).isEqualTo("changeStatus");
        assertThat(filter.get().getOperation()).isEqualTo(QueryOperator.NOT_EQUALS);
        assertThat(filter.get().getValue()).isEqualTo("implementation_complete");
        assertThat(filter.get().getValues()).isNull();
    }

    @Test
    void buildsFilterCriteriaWithLikeOperationWhenSingleValue() {

        final Optional<FilterCriteria> filter = FilterCriteria.build("managedObjectFdn:%NR149%");
        assertThat(filter).isPresent();
        assertThat(filter.get().getKey()).isEqualTo("managedObjectFdn");
        assertThat(filter.get().getOperation()).isEqualTo(QueryOperator.LIKE);
        assertThat(filter.get().getValue()).isEqualTo("nr149");
        assertThat(filter.get().getValues()).isNull();
    }

    @Test
    void buildsFilterCriteriaWithAnyOperationWhenFilterAsAWholeIsNull() {

        final Optional<FilterCriteria> filter = FilterCriteria.build(null);
        assertThat(filter).isEmpty();
    }

    @Test
    void buildsFilterCriteriaWithAnyOperationWhenFilterAsAWholeIsEmpty() {

        final Optional<FilterCriteria> filter = FilterCriteria.build(null);
        assertThat(filter).isEmpty();
    }

    @Test
    void buildsFilterCriteriaWithAnyOperationWhenFilterAttributeIsNull() {

        final Optional<FilterCriteria> filter = FilterCriteria.build(":attributeValue");
        assertThat(filter).isEmpty();
    }

    @Test
    void buildsFilterCriteriaWithAnyOperationWhenFilterValueIsNull() {

        final Optional<FilterCriteria> filter = FilterCriteria.build("attributeName:");
        assertThat(filter).isEmpty();
    }

    @Test
    void buildsFilterCriteriaWithLikeOperationForMangedObjectFdnAndPercentageNotPresentAtStart() {

        final Optional<FilterCriteria> filter = FilterCriteria.build("managedObjectFdn:value%");
        assertThat(filter).isPresent();
        assertThat(filter.get().getKey()).isEqualTo("managedObjectFdn");
        assertThat(filter.get().getOperation()).isEqualTo(QueryOperator.RIGHT_LIKE);
        assertThat(filter.get().getValue()).isEqualTo("value");
    }

    @Test
    void buildsFilterCriteriaWithLikeOperationForMangedObjectFdnAndPercentagePresentInAllPlace() {

        final Optional<FilterCriteria> filter = FilterCriteria.build("managedObjectFdn:%val%ue%%");
        assertThat(filter).isPresent();
        assertThat(filter.get().getKey()).isEqualTo("managedObjectFdn");
        assertThat(filter.get().getOperation()).isEqualTo(QueryOperator.LIKE);
        assertThat(filter.get().getValue()).isEqualTo("val%ue%");
    }

    @Test
    void buildsFilterCriteriaWithEqualOperationForMangedObjectFdnAndNoPercentagePresentOnEitherSideOfString() {

        final Optional<FilterCriteria> filter = FilterCriteria.build("managedObjectFdn:val%ue");
        assertThat(filter).isPresent();
        assertThat(filter.get().getKey()).isEqualTo("managedObjectFdn");
        assertThat(filter.get().getOperation()).isEqualTo(QueryOperator.EQUALS);
        assertThat(filter.get().getValue()).isEqualTo("val%ue");
    }

    @Test
    void buildsFilterCriteriaWithInOperationWhenMultipleValues() {
        final Optional<FilterCriteria> filter = FilterCriteria.build("changeStatus:(Applied,In Progress,Failed)");
        assertThat(filter).isPresent();
        assertThat(filter.get().getKey()).isEqualTo("changeStatus");
        assertThat(filter.get().getOperation()).isEqualTo(QueryOperator.IN);
        assertThat(filter.get().getValue()).isNull();
        assertThat(filter.get().getValues()).isEqualTo(List.of("Applied", "In Progress", "Failed"));

    }

    @Test
    void buildsNullFilterCriteriaWhenFilterIsNull() {
        final Optional<FilterCriteria> filter = FilterCriteria.build(null);
        assertThat(filter).isEmpty();
    }
}