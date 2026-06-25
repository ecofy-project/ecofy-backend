package br.com.ecofy.ms_budgeting.adapters.in.web.dto.response;

import br.com.ecofy.ms_budgeting.core.application.result.BudgetResult;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class BudgetResponseTest {

    private static final UUID ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID USER_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID CATEGORY_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final LocalDate PERIOD_START = LocalDate.of(2026, 6, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 6, 30);

    private static final Instant CREATED_AT =
            Instant.parse("2026-06-25T10:30:00Z");

    private static final Instant UPDATED_AT =
            Instant.parse("2026-06-25T11:30:00Z");

    @Test
    void shouldCreateBudgetResponseWithAllFields() {
        BudgetResponse response = new BudgetResponse(
                ID,
                USER_ID,
                CATEGORY_ID,
                "MONTHLY",
                PERIOD_START,
                PERIOD_END,
                "ACTIVE",
                "BRL",
                "1000.50",
                CREATED_AT,
                UPDATED_AT
        );

        assertEquals(ID, response.id());
        assertEquals(USER_ID, response.userId());
        assertEquals(CATEGORY_ID, response.categoryId());
        assertEquals("MONTHLY", response.periodType());
        assertEquals(PERIOD_START, response.periodStart());
        assertEquals(PERIOD_END, response.periodEnd());
        assertEquals("ACTIVE", response.status());
        assertEquals("BRL", response.currency());
        assertEquals("1000.50", response.limitAmount());
        assertEquals(CREATED_AT, response.createdAt());
        assertEquals(UPDATED_AT, response.updatedAt());
    }

    @Test
    void shouldCreateBudgetResponseWithNullFields() {
        BudgetResponse response = new BudgetResponse(
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
        assertNull(response.userId());
        assertNull(response.categoryId());
        assertNull(response.periodType());
        assertNull(response.periodStart());
        assertNull(response.periodEnd());
        assertNull(response.status());
        assertNull(response.currency());
        assertNull(response.limitAmount());
        assertNull(response.createdAt());
        assertNull(response.updatedAt());
    }

    @Test
    void shouldCreateBudgetResponseFromBudgetResult() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        BudgetResult result = mock(BudgetResult.class);

        doReturn(ID).when(result).id();
        doReturn(USER_ID).when(result).userId();
        doReturn(CATEGORY_ID).when(result).categoryId();
        doReturn(periodType).when(result).periodType();
        doReturn(PERIOD_START).when(result).periodStart();
        doReturn(PERIOD_END).when(result).periodEnd();
        doReturn(status).when(result).status();
        doReturn("BRL").when(result).currency();
        doReturn(new BigDecimal("1000.50")).when(result).limitAmount();
        doReturn(CREATED_AT).when(result).createdAt();
        doReturn(UPDATED_AT).when(result).updatedAt();

        BudgetResponse response = BudgetResponse.from(result);

        assertEquals(ID, response.id());
        assertEquals(USER_ID, response.userId());
        assertEquals(CATEGORY_ID, response.categoryId());
        assertEquals(periodType.name(), response.periodType());
        assertEquals(PERIOD_START, response.periodStart());
        assertEquals(PERIOD_END, response.periodEnd());
        assertEquals(status.name(), response.status());
        assertEquals("BRL", response.currency());
        assertEquals("1000.50", response.limitAmount());
        assertEquals(CREATED_AT, response.createdAt());
        assertEquals(UPDATED_AT, response.updatedAt());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenFromReceivesNullResult() {
        assertThrows(
                NullPointerException.class,
                () -> BudgetResponse.from(null)
        );
    }

    @Test
    void shouldThrowNullPointerExceptionWhenFromReceivesNullPeriodType() {
        BudgetResult result = mock(BudgetResult.class);

        doReturn(ID).when(result).id();
        doReturn(USER_ID).when(result).userId();
        doReturn(CATEGORY_ID).when(result).categoryId();
        doReturn(null).when(result).periodType();

        assertThrows(
                NullPointerException.class,
                () -> BudgetResponse.from(result)
        );
    }

    @Test
    void shouldThrowNullPointerExceptionWhenFromReceivesNullStatus() {
        BudgetResult result = mock(BudgetResult.class);

        doReturn(ID).when(result).id();
        doReturn(USER_ID).when(result).userId();
        doReturn(CATEGORY_ID).when(result).categoryId();
        doReturn(anyBudgetPeriodType()).when(result).periodType();
        doReturn(PERIOD_START).when(result).periodStart();
        doReturn(PERIOD_END).when(result).periodEnd();
        doReturn(null).when(result).status();

        assertThrows(
                NullPointerException.class,
                () -> BudgetResponse.from(result)
        );
    }

    @Test
    void shouldThrowNullPointerExceptionWhenFromReceivesNullLimitAmount() {
        BudgetResult result = mock(BudgetResult.class);

        doReturn(ID).when(result).id();
        doReturn(USER_ID).when(result).userId();
        doReturn(CATEGORY_ID).when(result).categoryId();
        doReturn(anyBudgetPeriodType()).when(result).periodType();
        doReturn(PERIOD_START).when(result).periodStart();
        doReturn(PERIOD_END).when(result).periodEnd();
        doReturn(anyBudgetStatus()).when(result).status();
        doReturn("BRL").when(result).currency();
        doReturn(null).when(result).limitAmount();

        assertThrows(
                NullPointerException.class,
                () -> BudgetResponse.from(result)
        );
    }

    @Test
    void shouldCompareBudgetResponseByAllRecordComponents() {
        BudgetResponse response = baseResponse();

        BudgetResponse sameResponse = baseResponse();

        BudgetResponse differentResponse = new BudgetResponse(
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                USER_ID,
                CATEGORY_ID,
                "MONTHLY",
                PERIOD_START,
                PERIOD_END,
                "ACTIVE",
                "BRL",
                "1000.50",
                CREATED_AT,
                UPDATED_AT
        );

        assertEquals(response, response);
        assertEquals(response, sameResponse);
        assertNotEquals(response, differentResponse);
        assertNotEquals(response, null);
        assertNotEquals(response, "not-a-budget-response");
    }

    @Test
    void shouldGenerateHashCodeUsingAllRecordComponents() {
        BudgetResponse response = baseResponse();
        BudgetResponse sameResponse = baseResponse();

        assertEquals(response, sameResponse);
        assertEquals(response.hashCode(), sameResponse.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenUserIdChanges() {
        BudgetResponse response = baseResponse();

        BudgetResponse differentResponse = new BudgetResponse(
                ID,
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                CATEGORY_ID,
                "MONTHLY",
                PERIOD_START,
                PERIOD_END,
                "ACTIVE",
                "BRL",
                "1000.50",
                CREATED_AT,
                UPDATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenCategoryIdChanges() {
        BudgetResponse response = baseResponse();

        BudgetResponse differentResponse = new BudgetResponse(
                ID,
                USER_ID,
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
                "MONTHLY",
                PERIOD_START,
                PERIOD_END,
                "ACTIVE",
                "BRL",
                "1000.50",
                CREATED_AT,
                UPDATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenPeriodTypeChanges() {
        BudgetResponse response = baseResponse();

        BudgetResponse differentResponse = new BudgetResponse(
                ID,
                USER_ID,
                CATEGORY_ID,
                "YEARLY",
                PERIOD_START,
                PERIOD_END,
                "ACTIVE",
                "BRL",
                "1000.50",
                CREATED_AT,
                UPDATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenPeriodStartChanges() {
        BudgetResponse response = baseResponse();

        BudgetResponse differentResponse = new BudgetResponse(
                ID,
                USER_ID,
                CATEGORY_ID,
                "MONTHLY",
                PERIOD_START.plusDays(1),
                PERIOD_END,
                "ACTIVE",
                "BRL",
                "1000.50",
                CREATED_AT,
                UPDATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenPeriodEndChanges() {
        BudgetResponse response = baseResponse();

        BudgetResponse differentResponse = new BudgetResponse(
                ID,
                USER_ID,
                CATEGORY_ID,
                "MONTHLY",
                PERIOD_START,
                PERIOD_END.plusDays(1),
                "ACTIVE",
                "BRL",
                "1000.50",
                CREATED_AT,
                UPDATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenStatusChanges() {
        BudgetResponse response = baseResponse();

        BudgetResponse differentResponse = new BudgetResponse(
                ID,
                USER_ID,
                CATEGORY_ID,
                "MONTHLY",
                PERIOD_START,
                PERIOD_END,
                "INACTIVE",
                "BRL",
                "1000.50",
                CREATED_AT,
                UPDATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenCurrencyChanges() {
        BudgetResponse response = baseResponse();

        BudgetResponse differentResponse = new BudgetResponse(
                ID,
                USER_ID,
                CATEGORY_ID,
                "MONTHLY",
                PERIOD_START,
                PERIOD_END,
                "ACTIVE",
                "USD",
                "1000.50",
                CREATED_AT,
                UPDATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenLimitAmountChanges() {
        BudgetResponse response = baseResponse();

        BudgetResponse differentResponse = new BudgetResponse(
                ID,
                USER_ID,
                CATEGORY_ID,
                "MONTHLY",
                PERIOD_START,
                PERIOD_END,
                "ACTIVE",
                "BRL",
                "2000.00",
                CREATED_AT,
                UPDATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenCreatedAtChanges() {
        BudgetResponse response = baseResponse();

        BudgetResponse differentResponse = new BudgetResponse(
                ID,
                USER_ID,
                CATEGORY_ID,
                "MONTHLY",
                PERIOD_START,
                PERIOD_END,
                "ACTIVE",
                "BRL",
                "1000.50",
                CREATED_AT.plusSeconds(60),
                UPDATED_AT
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenUpdatedAtChanges() {
        BudgetResponse response = baseResponse();

        BudgetResponse differentResponse = new BudgetResponse(
                ID,
                USER_ID,
                CATEGORY_ID,
                "MONTHLY",
                PERIOD_START,
                PERIOD_END,
                "ACTIVE",
                "BRL",
                "1000.50",
                CREATED_AT,
                UPDATED_AT.plusSeconds(60)
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldReturnToStringWithRecordComponents() {
        BudgetResponse response = baseResponse();

        String result = response.toString();

        assertTrue(result.contains("BudgetResponse"));
        assertTrue(result.contains("id=" + ID));
        assertTrue(result.contains("userId=" + USER_ID));
        assertTrue(result.contains("categoryId=" + CATEGORY_ID));
        assertTrue(result.contains("periodType=MONTHLY"));
        assertTrue(result.contains("periodStart=" + PERIOD_START));
        assertTrue(result.contains("periodEnd=" + PERIOD_END));
        assertTrue(result.contains("status=ACTIVE"));
        assertTrue(result.contains("currency=BRL"));
        assertTrue(result.contains("limitAmount=1000.50"));
        assertTrue(result.contains("createdAt=" + CREATED_AT));
        assertTrue(result.contains("updatedAt=" + UPDATED_AT));
    }

    private static BudgetResponse baseResponse() {
        return new BudgetResponse(
                ID,
                USER_ID,
                CATEGORY_ID,
                "MONTHLY",
                PERIOD_START,
                PERIOD_END,
                "ACTIVE",
                "BRL",
                "1000.50",
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static BudgetPeriodType anyBudgetPeriodType() {
        BudgetPeriodType[] values = BudgetPeriodType.values();

        if (values.length == 0) {
            throw new IllegalStateException("BudgetPeriodType enum must have at least one value");
        }

        return values[0];
    }

    private static BudgetStatus anyBudgetStatus() {
        BudgetStatus[] values = BudgetStatus.values();

        if (values.length == 0) {
            throw new IllegalStateException("BudgetStatus enum must have at least one value");
        }

        return values[0];
    }
}