package br.com.ecofy.ms_budgeting.adapters.in.web.dto.request;

import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetPeriodType;
import br.com.ecofy.ms_budgeting.core.domain.enums.BudgetStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CreateBudgetRequestTest {

    private static final UUID USER_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID CATEGORY_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final LocalDate PERIOD_START = LocalDate.of(2026, 6, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 6, 30);

    private final Validator validator = validator();

    @Test
    void shouldCreateCreateBudgetRequestWithAllFields() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        CreateBudgetRequest request = new CreateBudgetRequest(
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                BigDecimal.valueOf(1000.50),
                "BRL",
                status
        );

        assertEquals(USER_ID, request.userId());
        assertEquals(CATEGORY_ID, request.categoryId());
        assertEquals(periodType, request.periodType());
        assertEquals(PERIOD_START, request.periodStart());
        assertEquals(PERIOD_END, request.periodEnd());
        assertEquals(BigDecimal.valueOf(1000.50), request.limitAmount());
        assertEquals("BRL", request.currency());
        assertEquals(status, request.status());
    }

    @Test
    void shouldAcceptNullStatusBecauseStatusHasNoValidationAnnotation() {
        CreateBudgetRequest request = validRequest(null);

        Set<ConstraintViolation<CreateBudgetRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
        assertNull(request.status());
    }

    @Test
    void shouldPassValidationWhenRequestIsValidWithMinimumLimitAmount() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                USER_ID,
                CATEGORY_ID,
                anyBudgetPeriodType(),
                PERIOD_START,
                PERIOD_END,
                BigDecimal.valueOf(0.01),
                "BRL",
                anyBudgetStatus()
        );

        Set<ConstraintViolation<CreateBudgetRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldFailValidationWhenRequiredFieldsAreNull() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        Set<ConstraintViolation<CreateBudgetRequest>> violations =
                validator.validate(request);

        Set<String> fields = propertyNames(violations);

        assertEquals(7, violations.size());
        assertTrue(fields.contains("userId"));
        assertTrue(fields.contains("categoryId"));
        assertTrue(fields.contains("periodType"));
        assertTrue(fields.contains("periodStart"));
        assertTrue(fields.contains("periodEnd"));
        assertTrue(fields.contains("limitAmount"));
        assertTrue(fields.contains("currency"));

        assertFalse(fields.contains("status"));
    }

    @Test
    void shouldFailValidationWhenLimitAmountIsLowerThanMinimum() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                USER_ID,
                CATEGORY_ID,
                anyBudgetPeriodType(),
                PERIOD_START,
                PERIOD_END,
                BigDecimal.ZERO,
                "BRL",
                anyBudgetStatus()
        );

        Set<ConstraintViolation<CreateBudgetRequest>> violations =
                validator.validate(request);

        Set<String> fields = propertyNames(violations);

        assertEquals(1, violations.size());
        assertTrue(fields.contains("limitAmount"));
    }

    @Test
    void shouldFailValidationWhenCurrencyIsBlank() {
        CreateBudgetRequest request = new CreateBudgetRequest(
                USER_ID,
                CATEGORY_ID,
                anyBudgetPeriodType(),
                PERIOD_START,
                PERIOD_END,
                BigDecimal.valueOf(100),
                "   ",
                anyBudgetStatus()
        );

        Set<ConstraintViolation<CreateBudgetRequest>> violations =
                validator.validate(request);

        Set<String> fields = propertyNames(violations);

        assertEquals(1, violations.size());
        assertTrue(fields.contains("currency"));
    }

    @Test
    void shouldCompareCreateBudgetRequestByAllRecordComponents() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        CreateBudgetRequest request = new CreateBudgetRequest(
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                BigDecimal.valueOf(500),
                "BRL",
                status
        );

        CreateBudgetRequest sameRequest = new CreateBudgetRequest(
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                BigDecimal.valueOf(500),
                "BRL",
                status
        );

        CreateBudgetRequest differentRequest = new CreateBudgetRequest(
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                BigDecimal.valueOf(500),
                "BRL",
                status
        );

        assertEquals(request, request);
        assertEquals(request, sameRequest);
        assertNotEquals(request, differentRequest);
        assertNotEquals(request, null);
        assertNotEquals(request, "not-a-create-budget-request");
    }

    @Test
    void shouldGenerateHashCodeUsingAllRecordComponents() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        CreateBudgetRequest request = new CreateBudgetRequest(
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                BigDecimal.valueOf(500),
                "BRL",
                status
        );

        CreateBudgetRequest sameRequest = new CreateBudgetRequest(
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                BigDecimal.valueOf(500),
                "BRL",
                status
        );

        assertEquals(request, sameRequest);
        assertEquals(request.hashCode(), sameRequest.hashCode());
    }

    @Test
    void shouldReturnToStringWithRecordComponents() {
        BudgetPeriodType periodType = anyBudgetPeriodType();
        BudgetStatus status = anyBudgetStatus();

        CreateBudgetRequest request = new CreateBudgetRequest(
                USER_ID,
                CATEGORY_ID,
                periodType,
                PERIOD_START,
                PERIOD_END,
                BigDecimal.valueOf(500),
                "BRL",
                status
        );

        String result = request.toString();

        assertTrue(result.contains("CreateBudgetRequest"));
        assertTrue(result.contains("userId=" + USER_ID));
        assertTrue(result.contains("categoryId=" + CATEGORY_ID));
        assertTrue(result.contains("periodType=" + periodType));
        assertTrue(result.contains("periodStart=" + PERIOD_START));
        assertTrue(result.contains("periodEnd=" + PERIOD_END));
        assertTrue(result.contains("limitAmount=500"));
        assertTrue(result.contains("currency=BRL"));
        assertTrue(result.contains("status=" + status));
    }

    private static CreateBudgetRequest validRequest(BudgetStatus status) {
        return new CreateBudgetRequest(
                USER_ID,
                CATEGORY_ID,
                anyBudgetPeriodType(),
                PERIOD_START,
                PERIOD_END,
                BigDecimal.valueOf(100),
                "BRL",
                status
        );
    }

    private static Set<String> propertyNames(
            Set<ConstraintViolation<CreateBudgetRequest>> violations
    ) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static Validator validator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return factory.getValidator();
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