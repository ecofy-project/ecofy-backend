package br.com.ecofy.ms_budgeting.adapters.in.web.dto.response;

import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BudgetAlertResponseTest {

    private static final UUID ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID BUDGET_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID USER_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final UUID CATEGORY_ID =
            UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private static final Instant CREATED_AT =
            Instant.parse("2026-06-25T10:30:00Z");

    @Test
    void shouldCreateBudgetAlertResponseWithAllFields() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertResponse response = new BudgetAlertResponse(
                ID,
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                severity,
                "Budget reached 80% of the limit",
                80,
                80000L,
                100000L,
                "BRL",
                CREATED_AT
        );

        assertEquals(ID, response.id());
        assertEquals(BUDGET_ID, response.budgetId());
        assertEquals(USER_ID, response.userId());
        assertEquals(CATEGORY_ID, response.categoryId());
        assertEquals(severity, response.severity());
        assertEquals("Budget reached 80% of the limit", response.message());
        assertEquals(80, response.thresholdPercent());
        assertEquals(80000L, response.consumedCents());
        assertEquals(100000L, response.limitCents());
        assertEquals("BRL", response.currency());
        assertEquals(CREATED_AT, response.createdAt());
    }

    @Test
    void shouldCreateBudgetAlertResponseWithNullFields() {
        BudgetAlertResponse response = new BudgetAlertResponse(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertNull(response.id());
        assertNull(response.budgetId());
        assertNull(response.userId());
        assertNull(response.categoryId());
        assertNull(response.severity());
        assertNull(response.message());
        assertNull(response.thresholdPercent());
        assertNull(response.consumedCents());
        assertNull(response.limitCents());
        assertNull(response.currency());
        assertNull(response.createdAt());
    }

    @Test
    void shouldCompareBudgetAlertResponseByAllRecordComponents() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertResponse response = baseResponse(severity);

        BudgetAlertResponse sameResponse = baseResponse(severity);

        BudgetAlertResponse differentResponse = new BudgetAlertResponse(
                UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"),
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                severity,
                "Budget reached 80% of the limit",
                80,
                80000L,
                100000L,
                "BRL",
                CREATED_AT
        );

        assertEquals(response, response);
        assertEquals(response, sameResponse);
        assertNotEquals(response, differentResponse);
        assertNotEquals(response, null);
        assertNotEquals(response, "not-a-budget-alert-response");
    }

    @Test
    void shouldGenerateHashCodeUsingAllRecordComponents() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertResponse response = baseResponse(severity);
        BudgetAlertResponse sameResponse = baseResponse(severity);

        assertEquals(response, sameResponse);
        assertEquals(response.hashCode(), sameResponse.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenMessageChanges() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertResponse response = baseResponse(severity);

        BudgetAlertResponse differentResponse = new BudgetAlertResponse(
                ID,
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                severity,
                "Different message",
                80,
                80000L,
                100000L,
                "BRL",
                CREATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenThresholdPercentChanges() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertResponse response = baseResponse(severity);

        BudgetAlertResponse differentResponse = new BudgetAlertResponse(
                ID,
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                severity,
                "Budget reached 80% of the limit",
                90,
                80000L,
                100000L,
                "BRL",
                CREATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenConsumedCentsChanges() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertResponse response = baseResponse(severity);

        BudgetAlertResponse differentResponse = new BudgetAlertResponse(
                ID,
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                severity,
                "Budget reached 80% of the limit",
                80,
                90000L,
                100000L,
                "BRL",
                CREATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenLimitCentsChanges() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertResponse response = baseResponse(severity);

        BudgetAlertResponse differentResponse = new BudgetAlertResponse(
                ID,
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                severity,
                "Budget reached 80% of the limit",
                80,
                80000L,
                120000L,
                "BRL",
                CREATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenCurrencyChanges() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertResponse response = baseResponse(severity);

        BudgetAlertResponse differentResponse = new BudgetAlertResponse(
                ID,
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                severity,
                "Budget reached 80% of the limit",
                80,
                80000L,
                100000L,
                "USD",
                CREATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldReturnToStringWithRecordComponents() {
        AlertSeverity severity = anyAlertSeverity();

        BudgetAlertResponse response = baseResponse(severity);

        String result = response.toString();

        assertTrue(result.contains("BudgetAlertResponse"));
        assertTrue(result.contains("id=" + ID));
        assertTrue(result.contains("budgetId=" + BUDGET_ID));
        assertTrue(result.contains("userId=" + USER_ID));
        assertTrue(result.contains("categoryId=" + CATEGORY_ID));
        assertTrue(result.contains("severity=" + severity));
        assertTrue(result.contains("message=Budget reached 80% of the limit"));
        assertTrue(result.contains("thresholdPercent=80"));
        assertTrue(result.contains("consumedCents=80000"));
        assertTrue(result.contains("limitCents=100000"));
        assertTrue(result.contains("currency=BRL"));
        assertTrue(result.contains("createdAt=" + CREATED_AT));
    }

    private static BudgetAlertResponse baseResponse(AlertSeverity severity) {
        return new BudgetAlertResponse(
                ID,
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                severity,
                "Budget reached 80% of the limit",
                80,
                80000L,
                100000L,
                "BRL",
                CREATED_AT
        );
    }

    private static AlertSeverity anyAlertSeverity() {
        AlertSeverity[] values = AlertSeverity.values();

        if (values.length == 0) {
            throw new IllegalStateException("AlertSeverity enum must have at least one value");
        }

        return values[0];
    }
}