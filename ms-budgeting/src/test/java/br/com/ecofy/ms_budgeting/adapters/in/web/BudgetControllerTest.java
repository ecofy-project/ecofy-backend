package br.com.ecofy.ms_budgeting.adapters.in.web;

import br.com.ecofy.ms_budgeting.adapters.in.web.dto.request.CreateBudgetRequest;
import br.com.ecofy.ms_budgeting.adapters.in.web.dto.request.UpdateBudgetRequest;
import br.com.ecofy.ms_budgeting.adapters.in.web.dto.response.BudgetOverviewResponse;
import br.com.ecofy.ms_budgeting.adapters.in.web.dto.response.BudgetResponse;
import br.com.ecofy.ms_budgeting.core.application.command.CreateBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.command.DeleteBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.command.UpdateBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.result.BudgetOverviewResult;
import br.com.ecofy.ms_budgeting.core.application.result.BudgetResult;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import br.com.ecofy.ms_budgeting.core.port.in.CreateBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.DeleteBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.GetBudgetOverviewUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.GetBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.ListBudgetsUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.UpdateBudgetUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetControllerTest {

    private static final UUID BUDGET_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID SECOND_BUDGET_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID USER_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final UUID CATEGORY_ID =
            UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private static final LocalDate PERIOD_START = LocalDate.of(2026, 6, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 6, 30);

    private static final Instant CREATED_AT =
            Instant.parse("2026-06-25T10:30:00Z");

    private static final Instant UPDATED_AT =
            Instant.parse("2026-06-25T11:30:00Z");

    private static final BigDecimal LIMIT_AMOUNT = new BigDecimal("1000.50");

    @Mock
    private CreateBudgetUseCase createBudgetUseCase;

    @Mock
    private UpdateBudgetUseCase updateBudgetUseCase;

    @Mock
    private DeleteBudgetUseCase deleteBudgetUseCase;

    @Mock
    private ListBudgetsUseCase listBudgetsUseCase;

    @Mock
    private GetBudgetUseCase getBudgetUseCase;

    @Mock
    private GetBudgetOverviewUseCase getBudgetOverviewUseCase;

    private BudgetController controller;

    @BeforeEach
    void setUp() {
        controller = new BudgetController(
                createBudgetUseCase,
                updateBudgetUseCase,
                deleteBudgetUseCase,
                listBudgetsUseCase,
                getBudgetUseCase,
                getBudgetOverviewUseCase
        );
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldCreateBudgetWithIdempotencyKey() {
        bindCurrentRequest("POST", "/api/budgeting/v1/budgets");

        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        CreateBudgetRequest request = new CreateBudgetRequest(
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                LIMIT_AMOUNT,
                "BRL",
                status
        );

        BudgetResult createdResult = budgetResult(
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                status,
                "BRL",
                LIMIT_AMOUNT,
                CREATED_AT,
                UPDATED_AT
        );

        when(createBudgetUseCase.create(any(CreateBudgetCommand.class), eq("idem-key-001")))
                .thenReturn(createdResult);

        ResponseEntity<BudgetResponse> response =
                controller.create("idem-key-001", request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/api/budgeting/v1/budgets/" + BUDGET_ID));

        BudgetResponse body = response.getBody();

        assertNotNull(body);
        assertBudgetResponse(body, BUDGET_ID, USER_ID, CATEGORY_ID, periodType, status, LIMIT_AMOUNT);

        ArgumentCaptor<CreateBudgetCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateBudgetCommand.class);

        verify(createBudgetUseCase).create(commandCaptor.capture(), eq("idem-key-001"));

        CreateBudgetCommand command = commandCaptor.getValue();

        assertEquals(USER_ID, readAny(command, "userId"));
        assertEquals(CATEGORY_ID, readAny(command, "categoryId"));
        assertEquals(periodType, readAny(command, "periodType"));
        assertEquals(PERIOD_START, readAny(command, "periodStart"));
        assertEquals(PERIOD_END, readAny(command, "periodEnd"));
        assertEquals(LIMIT_AMOUNT, readAny(command, "limitAmount"));
        assertEquals("BRL", readAny(command, "currency"));
        assertEquals(status, readAny(command, "status"));
    }

    @Test
    void shouldCreateBudgetWithoutIdempotencyKey() {
        bindCurrentRequest("POST", "/api/budgeting/v1/budgets");

        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        CreateBudgetRequest request = new CreateBudgetRequest(
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                LIMIT_AMOUNT,
                "BRL",
                status
        );

        BudgetResult createdResult = budgetResult(
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                status,
                "BRL",
                LIMIT_AMOUNT,
                CREATED_AT,
                UPDATED_AT
        );

        when(createBudgetUseCase.create(any(CreateBudgetCommand.class), isNull()))
                .thenReturn(createdResult);

        ResponseEntity<BudgetResponse> response =
                controller.create(null, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(BUDGET_ID, response.getBody().id());

        verify(createBudgetUseCase).create(any(CreateBudgetCommand.class), isNull());
    }

    @Test
    void shouldUpdateBudgetWithIdempotencyKey() {
        BudgetStatus status = anyBudgetStatus();

        UpdateBudgetRequest request = new UpdateBudgetRequest(
                new BigDecimal("1500.75"),
                "USD",
                status
        );

        BudgetResult updatedResult = budgetResult(
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                anyBudgetPeriodType(),
                PERIOD_START,
                PERIOD_END,
                status,
                "USD",
                new BigDecimal("1500.75"),
                CREATED_AT,
                UPDATED_AT
        );

        when(updateBudgetUseCase.update(any(UpdateBudgetCommand.class), eq("idem-key-update")))
                .thenReturn(updatedResult);

        ResponseEntity<BudgetResponse> response =
                controller.update("idem-key-update", BUDGET_ID, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        BudgetResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(BUDGET_ID, body.id());
        assertEquals("USD", body.currency());
        assertEquals("1500.75", body.limitAmount());
        assertEquals(status.name(), body.status());

        ArgumentCaptor<UpdateBudgetCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateBudgetCommand.class);

        verify(updateBudgetUseCase).update(commandCaptor.capture(), eq("idem-key-update"));

        UpdateBudgetCommand command = commandCaptor.getValue();

        assertEquals(BUDGET_ID, readAny(command, "id", "budgetId"));
        assertEquals(new BigDecimal("1500.75"), readAny(command, "newLimitAmount", "limitAmount"));
        assertEquals("USD", readAny(command, "currency"));
        assertEquals(status, readAny(command, "status"));
    }

    @Test
    void shouldUpdateBudgetWithoutIdempotencyKey() {
        BudgetStatus status = anyBudgetStatus();

        UpdateBudgetRequest request = new UpdateBudgetRequest(
                new BigDecimal("2000.00"),
                "BRL",
                status
        );

        BudgetResult updatedResult = budgetResult(
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                anyBudgetPeriodType(),
                PERIOD_START,
                PERIOD_END,
                status,
                "BRL",
                new BigDecimal("2000.00"),
                CREATED_AT,
                UPDATED_AT
        );

        when(updateBudgetUseCase.update(any(UpdateBudgetCommand.class), isNull()))
                .thenReturn(updatedResult);

        ResponseEntity<BudgetResponse> response =
                controller.update(null, BUDGET_ID, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("2000.00", response.getBody().limitAmount());

        verify(updateBudgetUseCase).update(any(UpdateBudgetCommand.class), isNull());
    }

    @Test
    void shouldDeleteBudgetWithIdempotencyKey() {
        ResponseEntity<Void> response =
                controller.delete("idem-key-delete", BUDGET_ID);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        ArgumentCaptor<DeleteBudgetCommand> commandCaptor =
                ArgumentCaptor.forClass(DeleteBudgetCommand.class);

        verify(deleteBudgetUseCase).delete(commandCaptor.capture(), eq("idem-key-delete"));

        DeleteBudgetCommand command = commandCaptor.getValue();

        assertEquals(BUDGET_ID, readAny(command, "id", "budgetId"));
    }

    @Test
    void shouldDeleteBudgetWithoutIdempotencyKey() {
        ResponseEntity<Void> response =
                controller.delete(null, BUDGET_ID);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(deleteBudgetUseCase).delete(any(DeleteBudgetCommand.class), isNull());
    }

    @Test
    void shouldListBudgetsByUser() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        BudgetResult firstBudget = budgetResult(
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                status,
                "BRL",
                new BigDecimal("1000.50"),
                CREATED_AT,
                UPDATED_AT
        );

        BudgetResult secondBudget = budgetResult(
                SECOND_BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START.plusMonths(1),
                PERIOD_END.plusMonths(1),
                status,
                "BRL",
                new BigDecimal("2000.00"),
                CREATED_AT.plusSeconds(60),
                UPDATED_AT.plusSeconds(60)
        );

        when(listBudgetsUseCase.listByUser(USER_ID))
                .thenReturn(List.of(firstBudget, secondBudget));

        ResponseEntity<List<BudgetResponse>> response =
                controller.listByUser(USER_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<BudgetResponse> body = response.getBody();

        assertNotNull(body);
        assertEquals(2, body.size());

        assertEquals(BUDGET_ID, body.get(0).id());
        assertEquals(SECOND_BUDGET_ID, body.get(1).id());
        assertEquals(USER_ID, body.get(0).userId());
        assertEquals(USER_ID, body.get(1).userId());

        verify(listBudgetsUseCase).listByUser(USER_ID);
    }

    @Test
    void shouldGetBudgetById() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        BudgetResult budget = budgetResult(
                BUDGET_ID,
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                status,
                "BRL",
                LIMIT_AMOUNT,
                CREATED_AT,
                UPDATED_AT
        );

        when(getBudgetUseCase.get(BUDGET_ID))
                .thenReturn(budget);

        ResponseEntity<BudgetResponse> response =
                controller.get(BUDGET_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        BudgetResponse body = response.getBody();

        assertNotNull(body);
        assertBudgetResponse(body, BUDGET_ID, USER_ID, CATEGORY_ID, periodType, status, LIMIT_AMOUNT);

        verify(getBudgetUseCase).get(BUDGET_ID);
    }

    @Test
    void shouldReturnBudgetOverviewByUser() {
        List<?> consumptions = List.of("consumption-001", "consumption-002");
        List<?> alerts = List.of("alert-001", "alert-002");

        BudgetOverviewResult overviewResult = mock(BudgetOverviewResult.class);

        doReturn(USER_ID).when(overviewResult).userId();
        doReturn(consumptions).when(overviewResult).consumptions();
        doReturn(alerts).when(overviewResult).alerts();

        when(getBudgetOverviewUseCase.overview(USER_ID))
                .thenReturn(overviewResult);

        ResponseEntity<BudgetOverviewResponse> response =
                controller.overview(USER_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        BudgetOverviewResponse body = response.getBody();

        assertNotNull(body);
        assertEquals(USER_ID, body.userId());
        assertSame(consumptions, body.consumptions());
        assertSame(alerts, body.alerts());

        verify(getBudgetOverviewUseCase).overview(USER_ID);
    }

    private static BudgetResult budgetResult(
            UUID id,
            UUID userId,
            UUID categoryId,
            BudgetPeriodType periodType,
            LocalDate periodStart,
            LocalDate periodEnd,
            BudgetStatus status,
            String currency,
            BigDecimal limitAmount,
            Instant createdAt,
            Instant updatedAt
    ) {
        BudgetResult result = mock(BudgetResult.class);

        doReturn(id).when(result).id();
        doReturn(userId).when(result).userId();
        doReturn(categoryId).when(result).categoryId();
        doReturn(periodType).when(result).periodType();
        doReturn(periodStart).when(result).periodStart();
        doReturn(periodEnd).when(result).periodEnd();
        doReturn(status).when(result).status();
        doReturn(currency).when(result).currency();
        doReturn(limitAmount).when(result).limitAmount();
        doReturn(createdAt).when(result).createdAt();
        doReturn(updatedAt).when(result).updatedAt();

        return result;
    }

    private static void assertBudgetResponse(
            BudgetResponse response,
            UUID expectedId,
            UUID expectedUserId,
            UUID expectedCategoryId,
            BudgetPeriodType expectedPeriodType,
            BudgetStatus expectedStatus,
            BigDecimal expectedLimitAmount
    ) {
        assertEquals(expectedId, response.id());
        assertEquals(expectedUserId, response.userId());
        assertEquals(expectedCategoryId, response.categoryId());
        assertEquals(expectedPeriodType.name(), response.periodType());
        assertEquals(PERIOD_START, response.periodStart());
        assertEquals(PERIOD_END, response.periodEnd());
        assertEquals(expectedStatus.name(), response.status());
        assertEquals("BRL", response.currency());
        assertEquals(expectedLimitAmount.toPlainString(), response.limitAmount());
        assertEquals(CREATED_AT, response.createdAt());
        assertEquals(UPDATED_AT, response.updatedAt());
    }

    private static void bindCurrentRequest(String method, String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setContextPath("");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private static Object readAny(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // tenta o próximo nome possível
            } catch (Exception ex) {
                throw new AssertionError("Failed to read method: " + methodName, ex);
            }
        }

        throw new AssertionError(
                "None of the methods exist on "
                        + target.getClass().getName()
                        + ": "
                        + String.join(", ", methodNames)
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