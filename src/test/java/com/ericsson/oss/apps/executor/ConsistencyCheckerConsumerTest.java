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

package com.ericsson.oss.apps.executor;

import static com.ericsson.oss.apps.executor.ExecutionStatus.AUDIT_IN_PROGRESS;
import static com.ericsson.oss.apps.executor.ExecutionStatus.AUDIT_SUCCESSFUL;
import static com.ericsson.oss.apps.executor.ExecutionType.OPEN_LOOP;
import static com.ericsson.oss.apps.util.DateUtil.getCurrentUtcDateTime;
import static com.ericsson.oss.apps.util.TestDefaults.DEFAULT_JOB_NAME;
import static com.ericsson.oss.apps.util.TestDefaults.MANAGED_ELEMENT_1_FDN;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import com.ericsson.assertions.exception.ControllerDetailExceptionSoftAssertions;
import com.ericsson.oss.apps.model.EaccManagedObject;
import com.ericsson.oss.apps.model.Execution;
import com.ericsson.oss.apps.model.Rule;
import com.ericsson.oss.apps.repository.AuditResultsRepository;
import com.ericsson.oss.apps.repository.ExecutionsRepository;
import com.ericsson.oss.apps.service.MetricService;

import lombok.extern.slf4j.Slf4j;

/**
 * Unit tests for {@link ConsistencyCheckerConsumer} class.
 */
@Slf4j
@ExtendWith({ SoftAssertionsExtension.class, MockitoExtension.class })
class ConsistencyCheckerConsumerTest {

    @InjectSoftAssertions
    private ControllerDetailExceptionSoftAssertions softly;
    BlockingQueue<EaccManagedObject> eaccManagedObjectQueue = new LinkedBlockingDeque<>();
    private final Map<String, List<Rule>> ruleSet = new HashMap<>();

    @Mock
    Execution execution;
    @Mock
    AuditResultsRepository auditResultsRepository;
    @Mock
    ExecutionsRepository executionsRepository;
    @Mock
    MetricService metricService;

    private ConsistencyCheckerConsumer objectUnderTest;

    @BeforeEach
    public void setup() {
        objectUnderTest = new ConsistencyCheckerConsumer(eaccManagedObjectQueue, ruleSet, execution,
                auditResultsRepository, executionsRepository, metricService, 1);
    }



    @Test
    void whenNoManagedObjectInBlockingQueue_thenCheckIsSuccessfulAndAllCountsAreZero() {
        objectUnderTest.finish(); // no items will be added so indicate no more are expected in the queue

        softly.assertThat(objectUnderTest.waitForCompletion()).isTrue();
        softly.assertThat(objectUnderTest.getTotalManagedObjects()).isZero();
        softly.assertThat(objectUnderTest.getTotalAuditResults()).isZero();
        softly.assertThat(objectUnderTest.getTotalInconsistencies()).isZero();
    }

    @Test
    void whenManagedObjectInQueue_andThereIsNoRulesForTheManagedObject_thenCheckIsSuccessfulAndCountsAreCorrect() throws InterruptedException {

        final EaccManagedObject eaccManagedObject = createEaccManagedObject();
        eaccManagedObjectQueue.put(eaccManagedObject);
        objectUnderTest.finish();

        softly.assertThat(objectUnderTest.waitForCompletion()).isTrue();
        softly.assertThat(objectUnderTest.getTotalManagedObjects()).isEqualTo(1);
        softly.assertThat(objectUnderTest.getTotalAuditResults()).isZero();
        softly.assertThat(objectUnderTest.getTotalInconsistencies()).isZero();

    }

    @Test
    void whenManagedObjectInQueueIsChecked_andAnExceptionOccurs_thenCheckIsUnsuccessfulAndAllCountsAreZero() throws InterruptedException {
        when(auditResultsRepository.saveAll(any())).thenThrow(new IllegalStateException("Some exception"));

        final EaccManagedObject eaccManagedObject = createEaccManagedObject();
        eaccManagedObjectQueue.put(eaccManagedObject);
        objectUnderTest.finish();

        softly.assertThat(objectUnderTest.waitForCompletion()).isFalse();
        softly.assertThat(objectUnderTest.getTotalManagedObjects()).isZero();
        softly.assertThat(objectUnderTest.getTotalAuditResults()).isZero();
        softly.assertThat(objectUnderTest.getTotalInconsistencies()).isZero();

    }

    @Test
    void whenManagedObjectInQueueIsChecked_andAnInterruptExceptionOccurs_thenCheckIsUnsuccessfulAndThreadIsMarkedAsInterrupted() {
        objectUnderTest.finish();

        Thread.currentThread().interrupt();
        softly.assertThat(objectUnderTest.waitForCompletion()).isFalse();
        softly.assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    private EaccManagedObject createEaccManagedObject() {
        final EaccManagedObject eaccManagedObject = new EaccManagedObject();
        eaccManagedObject.setFdn(MANAGED_ELEMENT_1_FDN);
        eaccManagedObject.setMoType("ManagedElement");
        eaccManagedObject.setMoId("1");
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put("attribute1", 1);
        eaccManagedObject.setAttributes(attributes);
        return eaccManagedObject;
    }
}