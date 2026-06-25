package br.com.ecofy.ms_budgeting.adapters.in.web.dto.response;

import br.com.ecofy.ms_budgeting.core.application.result.BudgetOverviewResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class BudgetOverviewResponseTest {

    private static final UUID USER_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void shouldCreateBudgetOverviewResponseWithAllFields() {
        List<?> consumptions = List.of("consumption-001", "consumption-002");
        List<?> alerts = List.of("alert-001", "alert-002");

        BudgetOverviewResponse response = new BudgetOverviewResponse(
                USER_ID,
                consumptions,
                alerts
        );

        assertEquals(USER_ID, response.userId());
        assertEquals(consumptions, response.consumptions());
        assertEquals(alerts, response.alerts());
    }

    @Test
    void shouldCreateBudgetOverviewResponseWithNullFields() {
        BudgetOverviewResponse response = new BudgetOverviewResponse(
                null,
                null,
                null
        );

        assertNull(response.userId());
        assertNull(response.consumptions());
        assertNull(response.alerts());
    }

    @Test
    void shouldCreateBudgetOverviewResponseFromBudgetOverviewResult() {
        List<?> consumptions = List.of("consumption-001", "consumption-002");
        List<?> alerts = List.of("alert-001", "alert-002");

        BudgetOverviewResult result = mock(BudgetOverviewResult.class);

        doReturn(USER_ID).when(result).userId();
        doReturn(consumptions).when(result).consumptions();
        doReturn(alerts).when(result).alerts();

        BudgetOverviewResponse response = BudgetOverviewResponse.from(result);

        assertEquals(USER_ID, response.userId());
        assertSame(consumptions, response.consumptions());
        assertSame(alerts, response.alerts());
    }

    @Test
    void shouldThrowNullPointerExceptionWhenFromReceivesNullResult() {
        assertThrows(
                NullPointerException.class,
                () -> BudgetOverviewResponse.from(null)
        );
    }

    @Test
    void shouldCompareBudgetOverviewResponseByAllRecordComponents() {
        List<?> consumptions = List.of("consumption-001");
        List<?> alerts = List.of("alert-001");

        BudgetOverviewResponse response = new BudgetOverviewResponse(
                USER_ID,
                consumptions,
                alerts
        );

        BudgetOverviewResponse sameResponse = new BudgetOverviewResponse(
                USER_ID,
                consumptions,
                alerts
        );

        BudgetOverviewResponse differentResponse = new BudgetOverviewResponse(
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                consumptions,
                alerts
        );

        assertEquals(response, response);
        assertEquals(response, sameResponse);
        assertNotEquals(response, differentResponse);
        assertNotEquals(response, null);
        assertNotEquals(response, "not-a-budget-overview-response");
    }

    @Test
    void shouldGenerateHashCodeUsingAllRecordComponents() {
        List<?> consumptions = List.of("consumption-001");
        List<?> alerts = List.of("alert-001");

        BudgetOverviewResponse response = new BudgetOverviewResponse(
                USER_ID,
                consumptions,
                alerts
        );

        BudgetOverviewResponse sameResponse = new BudgetOverviewResponse(
                USER_ID,
                consumptions,
                alerts
        );

        assertEquals(response, sameResponse);
        assertEquals(response.hashCode(), sameResponse.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenConsumptionsChange() {
        BudgetOverviewResponse response = new BudgetOverviewResponse(
                USER_ID,
                List.of("consumption-001"),
                List.of("alert-001")
        );

        BudgetOverviewResponse differentResponse = new BudgetOverviewResponse(
                USER_ID,
                List.of("consumption-002"),
                List.of("alert-001")
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldNotBeEqualWhenAlertsChange() {
        BudgetOverviewResponse response = new BudgetOverviewResponse(
                USER_ID,
                List.of("consumption-001"),
                List.of("alert-001")
        );

        BudgetOverviewResponse differentResponse = new BudgetOverviewResponse(
                USER_ID,
                List.of("consumption-001"),
                List.of("alert-002")
        );

        assertNotEquals(response, differentResponse);
    }

    @Test
    void shouldReturnToStringWithRecordComponents() {
        List<?> consumptions = List.of("consumption-001");
        List<?> alerts = List.of("alert-001");

        BudgetOverviewResponse response = new BudgetOverviewResponse(
                USER_ID,
                consumptions,
                alerts
        );

        String result = response.toString();

        assertTrue(result.contains("BudgetOverviewResponse"));
        assertTrue(result.contains("userId=" + USER_ID));
        assertTrue(result.contains("consumptions=" + consumptions));
        assertTrue(result.contains("alerts=" + alerts));
    }
}