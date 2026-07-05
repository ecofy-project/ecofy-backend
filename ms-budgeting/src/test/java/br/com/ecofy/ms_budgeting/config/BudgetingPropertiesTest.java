package br.com.ecofy.ms_budgeting.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class BudgetingPropertiesTest {

    private static final String CATEGORIZED_TRANSACTION_TOPIC =
            "budgeting.categorized-transaction";

    private static final String BUDGET_ALERT_TOPIC =
            "budgeting.budget-alert";

    private static final Duration IDEMPOTENCY_TTL =
            Duration.ofMinutes(10);

    private static final BigDecimal WARNING_THRESHOLD =
            new BigDecimal("0.80");

    private static final BigDecimal CRITICAL_THRESHOLD =
            new BigDecimal("0.95");

    private static final Integer CLEANUP_RETENTION_DAYS = 90;

    private static final String CLEANUP_CRON =
            "0 0 3 * * *";

    private static final String RECALCULATE_CRON =
            "0 0/30 * * * *";

    @Test
    void shouldCreateBudgetingPropertiesWithAllNestedRecords() {
        BudgetingProperties.Topics topics = topics();
        BudgetingProperties.Idempotency idempotency = idempotency();
        BudgetingProperties.Alerts alerts = alerts(true);
        BudgetingProperties.Schedulers schedulers = schedulers(true, true);
        BudgetingProperties.Scheduling scheduling = scheduling();

        BudgetingProperties properties = new BudgetingProperties(
                topics,
                idempotency,
                alerts,
                schedulers,
                scheduling
        );

        assertSame(topics, properties.topics());
        assertSame(idempotency, properties.idempotency());
        assertSame(alerts, properties.alerts());
        assertSame(schedulers, properties.schedulers());
        assertSame(scheduling, properties.scheduling());
    }

    @Test
    void shouldCreateBudgetingPropertiesWithNullNestedRecords() {
        BudgetingProperties properties = new BudgetingProperties(
                null,
                null,
                null,
                null,
                null
        );

        assertNull(properties.topics());
        assertNull(properties.idempotency());
        assertNull(properties.alerts());
        assertNull(properties.schedulers());
        assertNull(properties.scheduling());
    }

    @Test
    void shouldCreateTopicsRecord() {
        BudgetingProperties.Topics topics = new BudgetingProperties.Topics(
                CATEGORIZED_TRANSACTION_TOPIC,
                BUDGET_ALERT_TOPIC
        );

        assertEquals(CATEGORIZED_TRANSACTION_TOPIC, topics.categorizedTransaction());
        assertEquals(BUDGET_ALERT_TOPIC, topics.budgetAlert());
    }

    @Test
    void shouldCreateTopicsRecordWithNullFields() {
        BudgetingProperties.Topics topics = new BudgetingProperties.Topics(
                null,
                null
        );

        assertNull(topics.categorizedTransaction());
        assertNull(topics.budgetAlert());
    }

    @Test
    void shouldCreateIdempotencyRecord() {
        BudgetingProperties.Idempotency idempotency =
                new BudgetingProperties.Idempotency(IDEMPOTENCY_TTL);

        assertEquals(IDEMPOTENCY_TTL, idempotency.ttl());
    }

    @Test
    void shouldCreateIdempotencyRecordWithNullTtl() {
        BudgetingProperties.Idempotency idempotency =
                new BudgetingProperties.Idempotency(null);

        assertNull(idempotency.ttl());
    }

    @Test
    void shouldCreateAlertsRecordWithPublishOnEveryUpdateEnabled() {
        BudgetingProperties.Alerts alerts = new BudgetingProperties.Alerts(
                WARNING_THRESHOLD,
                CRITICAL_THRESHOLD,
                true
        );

        assertEquals(WARNING_THRESHOLD, alerts.warningThresholdPct());
        assertEquals(CRITICAL_THRESHOLD, alerts.criticalThresholdPct());
        assertTrue(alerts.publishOnEveryUpdate());
    }

    @Test
    void shouldCreateAlertsRecordWithPublishOnEveryUpdateDisabled() {
        BudgetingProperties.Alerts alerts = new BudgetingProperties.Alerts(
                WARNING_THRESHOLD,
                CRITICAL_THRESHOLD,
                false
        );

        assertEquals(WARNING_THRESHOLD, alerts.warningThresholdPct());
        assertEquals(CRITICAL_THRESHOLD, alerts.criticalThresholdPct());
        assertFalse(alerts.publishOnEveryUpdate());
    }

    @Test
    void shouldCreateAlertsRecordWithNullThresholds() {
        BudgetingProperties.Alerts alerts = new BudgetingProperties.Alerts(
                null,
                null,
                true
        );

        assertNull(alerts.warningThresholdPct());
        assertNull(alerts.criticalThresholdPct());
        assertTrue(alerts.publishOnEveryUpdate());
    }

    @Test
    void shouldCreateSchedulersRecordWithSchedulersEnabled() {
        BudgetingProperties.Schedulers schedulers =
                new BudgetingProperties.Schedulers(true, true);

        assertTrue(schedulers.recalculationEnabled());
        assertTrue(schedulers.cleanupEnabled());
    }

    @Test
    void shouldCreateSchedulersRecordWithSchedulersDisabled() {
        BudgetingProperties.Schedulers schedulers =
                new BudgetingProperties.Schedulers(false, false);

        assertFalse(schedulers.recalculationEnabled());
        assertFalse(schedulers.cleanupEnabled());
    }

    @Test
    void shouldCreateSchedulersRecordWithMixedFlags() {
        BudgetingProperties.Schedulers schedulers =
                new BudgetingProperties.Schedulers(true, false);

        assertTrue(schedulers.recalculationEnabled());
        assertFalse(schedulers.cleanupEnabled());
    }

    @Test
    void shouldCreateSchedulingRecord() {
        BudgetingProperties.Scheduling scheduling = new BudgetingProperties.Scheduling(
                CLEANUP_RETENTION_DAYS,
                CLEANUP_CRON,
                RECALCULATE_CRON
        );

        assertEquals(CLEANUP_RETENTION_DAYS, scheduling.cleanupRetentionDays());
        assertEquals(CLEANUP_CRON, scheduling.cleanupCron());
        assertEquals(RECALCULATE_CRON, scheduling.recalculateCron());
    }

    @Test
    void shouldCreateSchedulingRecordWithNullFields() {
        BudgetingProperties.Scheduling scheduling = new BudgetingProperties.Scheduling(
                null,
                null,
                null
        );

        assertNull(scheduling.cleanupRetentionDays());
        assertNull(scheduling.cleanupCron());
        assertNull(scheduling.recalculateCron());
    }

    @Test
    void shouldHaveConfigurationPropertiesAnnotationWithExpectedPrefix() {
        ConfigurationProperties annotation =
                BudgetingProperties.class.getAnnotation(ConfigurationProperties.class);

        assertNotNull(annotation);
        assertEquals("ecofy.budgeting", annotation.prefix());
    }

    @Test
    void shouldCompareBudgetingPropertiesByAllComponents() {
        BudgetingProperties first = properties();
        BudgetingProperties same = properties();

        BudgetingProperties different = new BudgetingProperties(
                new BudgetingProperties.Topics("another-topic", BUDGET_ALERT_TOPIC),
                idempotency(),
                alerts(true),
                schedulers(true, true),
                scheduling()
        );

        assertEquals(first, first);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());

        assertNotEquals(first, different);
        assertNotEquals(first, null);
        assertNotEquals(first, "not-a-budgeting-properties");
    }

    @Test
    void shouldCompareTopicsByAllComponents() {
        BudgetingProperties.Topics first = topics();
        BudgetingProperties.Topics same = topics();

        BudgetingProperties.Topics differentCategorizedTransaction =
                new BudgetingProperties.Topics("another-topic", BUDGET_ALERT_TOPIC);

        BudgetingProperties.Topics differentBudgetAlert =
                new BudgetingProperties.Topics(CATEGORIZED_TRANSACTION_TOPIC, "another-alert-topic");

        assertEquals(first, first);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());

        assertNotEquals(first, differentCategorizedTransaction);
        assertNotEquals(first, differentBudgetAlert);
        assertNotEquals(first, null);
        assertNotEquals(first, "not-topics");
    }

    @Test
    void shouldCompareIdempotencyByAllComponents() {
        BudgetingProperties.Idempotency first = idempotency();
        BudgetingProperties.Idempotency same = idempotency();

        BudgetingProperties.Idempotency different =
                new BudgetingProperties.Idempotency(Duration.ofMinutes(20));

        assertEquals(first, first);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());

        assertNotEquals(first, different);
        assertNotEquals(first, null);
        assertNotEquals(first, "not-idempotency");
    }

    @Test
    void shouldCompareAlertsByAllComponents() {
        BudgetingProperties.Alerts first = alerts(true);
        BudgetingProperties.Alerts same = alerts(true);

        BudgetingProperties.Alerts differentWarning =
                new BudgetingProperties.Alerts(new BigDecimal("0.70"), CRITICAL_THRESHOLD, true);

        BudgetingProperties.Alerts differentCritical =
                new BudgetingProperties.Alerts(WARNING_THRESHOLD, new BigDecimal("0.99"), true);

        BudgetingProperties.Alerts differentFlag =
                new BudgetingProperties.Alerts(WARNING_THRESHOLD, CRITICAL_THRESHOLD, false);

        assertEquals(first, first);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());

        assertNotEquals(first, differentWarning);
        assertNotEquals(first, differentCritical);
        assertNotEquals(first, differentFlag);
        assertNotEquals(first, null);
        assertNotEquals(first, "not-alerts");
    }

    @Test
    void shouldCompareSchedulersByAllComponents() {
        BudgetingProperties.Schedulers first = schedulers(true, true);
        BudgetingProperties.Schedulers same = schedulers(true, true);

        BudgetingProperties.Schedulers differentRecalculation =
                schedulers(false, true);

        BudgetingProperties.Schedulers differentCleanup =
                schedulers(true, false);

        assertEquals(first, first);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());

        assertNotEquals(first, differentRecalculation);
        assertNotEquals(first, differentCleanup);
        assertNotEquals(first, null);
        assertNotEquals(first, "not-schedulers");
    }

    @Test
    void shouldCompareSchedulingByAllComponents() {
        BudgetingProperties.Scheduling first = scheduling();
        BudgetingProperties.Scheduling same = scheduling();

        BudgetingProperties.Scheduling differentRetention =
                new BudgetingProperties.Scheduling(30, CLEANUP_CRON, RECALCULATE_CRON);

        BudgetingProperties.Scheduling differentCleanupCron =
                new BudgetingProperties.Scheduling(CLEANUP_RETENTION_DAYS, "0 0 1 * * *", RECALCULATE_CRON);

        BudgetingProperties.Scheduling differentRecalculateCron =
                new BudgetingProperties.Scheduling(CLEANUP_RETENTION_DAYS, CLEANUP_CRON, "0 0 6 * * *");

        assertEquals(first, first);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());

        assertNotEquals(first, differentRetention);
        assertNotEquals(first, differentCleanupCron);
        assertNotEquals(first, differentRecalculateCron);
        assertNotEquals(first, null);
        assertNotEquals(first, "not-scheduling");
    }

    @Test
    void shouldReturnToStringWithBudgetingPropertiesComponents() {
        String result = properties().toString();

        assertTrue(result.contains("BudgetingProperties"));
        assertTrue(result.contains("topics="));
        assertTrue(result.contains("idempotency="));
        assertTrue(result.contains("alerts="));
        assertTrue(result.contains("schedulers="));
        assertTrue(result.contains("scheduling="));
    }

    @Test
    void shouldReturnToStringWithTopicsComponents() {
        String result = topics().toString();

        assertTrue(result.contains("Topics"));
        assertTrue(result.contains("categorizedTransaction=" + CATEGORIZED_TRANSACTION_TOPIC));
        assertTrue(result.contains("budgetAlert=" + BUDGET_ALERT_TOPIC));
    }

    @Test
    void shouldReturnToStringWithIdempotencyComponents() {
        String result = idempotency().toString();

        assertTrue(result.contains("Idempotency"));
        assertTrue(result.contains("ttl=" + IDEMPOTENCY_TTL));
    }

    @Test
    void shouldReturnToStringWithAlertsComponents() {
        String result = alerts(true).toString();

        assertTrue(result.contains("Alerts"));
        assertTrue(result.contains("warningThresholdPct=" + WARNING_THRESHOLD));
        assertTrue(result.contains("criticalThresholdPct=" + CRITICAL_THRESHOLD));
        assertTrue(result.contains("publishOnEveryUpdate=true"));
    }

    @Test
    void shouldReturnToStringWithSchedulersComponents() {
        String result = schedulers(true, false).toString();

        assertTrue(result.contains("Schedulers"));
        assertTrue(result.contains("recalculationEnabled=true"));
        assertTrue(result.contains("cleanupEnabled=false"));
    }

    @Test
    void shouldReturnToStringWithSchedulingComponents() {
        String result = scheduling().toString();

        assertTrue(result.contains("Scheduling"));
        assertTrue(result.contains("cleanupRetentionDays=" + CLEANUP_RETENTION_DAYS));
        assertTrue(result.contains("cleanupCron=" + CLEANUP_CRON));
        assertTrue(result.contains("recalculateCron=" + RECALCULATE_CRON));
    }

    private static BudgetingProperties properties() {
        return new BudgetingProperties(
                topics(),
                idempotency(),
                alerts(true),
                schedulers(true, true),
                scheduling()
        );
    }

    private static BudgetingProperties.Topics topics() {
        return new BudgetingProperties.Topics(
                CATEGORIZED_TRANSACTION_TOPIC,
                BUDGET_ALERT_TOPIC
        );
    }

    private static BudgetingProperties.Idempotency idempotency() {
        return new BudgetingProperties.Idempotency(IDEMPOTENCY_TTL);
    }

    private static BudgetingProperties.Alerts alerts(boolean publishOnEveryUpdate) {
        return new BudgetingProperties.Alerts(
                WARNING_THRESHOLD,
                CRITICAL_THRESHOLD,
                publishOnEveryUpdate
        );
    }

    private static BudgetingProperties.Schedulers schedulers(
            boolean recalculationEnabled,
            boolean cleanupEnabled
    ) {
        return new BudgetingProperties.Schedulers(
                recalculationEnabled,
                cleanupEnabled
        );
    }

    private static BudgetingProperties.Scheduling scheduling() {
        return new BudgetingProperties.Scheduling(
                CLEANUP_RETENTION_DAYS,
                CLEANUP_CRON,
                RECALCULATE_CRON
        );
    }
}