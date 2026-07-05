package br.com.ecofy.ms_budgeting.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class BeanConfigTest {

    @Test
    void shouldCreateBeanConfigInstance() {
        BeanConfig config = new BeanConfig();

        assertNotNull(config);
    }

    @Test
    void shouldReturnSystemDefaultZoneClock() {
        BeanConfig config = new BeanConfig();

        Clock clock = config.clock();

        assertNotNull(clock);
        assertEquals(ZoneId.systemDefault(), clock.getZone());
    }

    @Test
    void shouldReturnClockThatCanProduceInstant() {
        BeanConfig config = new BeanConfig();

        Clock clock = config.clock();

        assertNotNull(clock.instant());
    }

    @Test
    void shouldHaveConfigurationAnnotation() {
        Configuration annotation = BeanConfig.class.getAnnotation(Configuration.class);

        assertNotNull(annotation);
    }

    @Test
    void shouldHaveBeanAnnotationOnClockMethod() throws Exception {
        Method method = BeanConfig.class.getDeclaredMethod("clock");

        Bean annotation = method.getAnnotation(Bean.class);

        assertNotNull(annotation);
    }

    @Test
    void shouldClockMethodReturnClockType() throws Exception {
        Method method = BeanConfig.class.getDeclaredMethod("clock");

        assertEquals(Clock.class, method.getReturnType());
    }
}