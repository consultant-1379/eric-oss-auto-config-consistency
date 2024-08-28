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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;

import com.ericsson.oss.apps.model.AuditResult;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;

/**
 * Builds query for single value and multi-value filter persisted as <code>Enums</code>. <br>
 * Can be extended as we need to support different operations and data types <br>
 * <b>Note:</b> Current implementation will work for single String value also.
 */
@Slf4j
public class AuditResultsSpecification implements Specification<AuditResult> {
    private static final long serialVersionUID = 1905122041950251207L;
    private final transient Optional<FilterCriteria> criteria;

    public AuditResultsSpecification(final Optional<FilterCriteria> criteria) {
        this.criteria = criteria;
    }

    @Override
    public Predicate toPredicate(final Root<AuditResult> root, final CriteriaQuery<?> query, final CriteriaBuilder criteriaBuilder) {
        if (criteria.isEmpty()) {
            return null;
        }
        final FilterCriteria filterCriteria = criteria.get();
        switch (criteria.get().getOperation()) {
            case EQUALS:
                if (Enum.class.isAssignableFrom(root.get(filterCriteria.getKey()).getJavaType())) {
                    return criteriaBuilder.equal(root.<String>get(filterCriteria.getKey()),
                            Enum.valueOf(root.<Enum>get(filterCriteria.getKey()).getJavaType(),
                                    filterCriteria.getValue().toString().toUpperCase(Locale.ROOT)));
                } else {
                    return criteriaBuilder.equal(root.get(filterCriteria.getKey()), filterCriteria.getValue());
                }
            case NOT_EQUALS:
                if (Enum.class.isAssignableFrom(root.get(filterCriteria.getKey()).getJavaType())) {
                    return criteriaBuilder.notEqual(root.<String>get(filterCriteria.getKey()),
                            Enum.valueOf(root.<Enum>get(filterCriteria.getKey()).getJavaType(),
                                    filterCriteria.getValue().toString().toUpperCase(Locale.ROOT)));
                } else {
                    return criteriaBuilder.notEqual(root.get(filterCriteria.getKey()), filterCriteria.getValue());
                }
            case IN:
                if (Enum.class.isAssignableFrom(root.get(filterCriteria.getKey()).getJavaType())) {
                    final List<Enum> list = new ArrayList<>();
                    for (final String value : filterCriteria.getValues()) {
                        list.add(Enum.valueOf(root.<Enum>get(filterCriteria.getKey()).getJavaType(),
                                value.toUpperCase(Locale.ROOT).replace(" ", "_")));
                    }
                    return criteriaBuilder.in(root.get(filterCriteria.getKey()))
                            .value(list);
                }
                return null;

            case LIKE:
                return criteriaBuilder.like(criteriaBuilder.lower(root.get(filterCriteria.getKey())), "%" + filterCriteria.getValue() + "%");
            case RIGHT_LIKE:
                return criteriaBuilder.like(criteriaBuilder.lower(root.get(filterCriteria.getKey())), filterCriteria.getValue() + "%");
            case LEFT_LIKE:
                return criteriaBuilder.like(criteriaBuilder.lower(root.get(filterCriteria.getKey())), "%" + filterCriteria.getValue());

            default:
                return null;
        }
    }
}
