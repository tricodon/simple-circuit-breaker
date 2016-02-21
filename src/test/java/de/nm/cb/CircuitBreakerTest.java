package de.nm.cb;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


public class CircuitBreakerTest {

    private CircuitBreakerExecutor circuitBreakerExecutor;
    private Clock clock;
    private Instant start;

    @Before
    public void setUp() {
        clock = mock(Clock.class);
        start = Instant.now();
        when(clock.instant()).thenReturn(start);

        circuitBreakerExecutor = new CircuitBreakerExecutor(clock);
    }

    @Test
    public void success() {
        CircuitBreakerTask<String> task = new CircuitBreakerTask<String>() {
            @Override
            public String processAndReturn() {
                return "success";
            }

            @Override
            public String getFallback() {
                return "fallback";
            }
        };

        assertThat(circuitBreakerExecutor.execute(task)).isEqualTo("success");
        assertThat(circuitBreakerExecutor.getCurrentNumberOfErrors()).isEqualTo(0);
    }

    @Test
    public void fallbackOnError() {
        CircuitBreakerTask<String> task = new CircuitBreakerTask<String>() {
            @Override
            public String processAndReturn() {
                throw new RuntimeException("error");
            }

            @Override
            public String getFallback() {
                return "fallback";
            }
        };

        assertThat(circuitBreakerExecutor.execute(task)).isEqualTo("fallback");
        assertThat(circuitBreakerExecutor.getCurrentNumberOfErrors()).isEqualTo(1);
    }

    @Test
    public void openAfterTenErrorsDirectly() {
        circuitBreakerExecutor.setOpenAfterErrorsThreshold(10);
        CircuitBreakerTask<String> mock = mock(CircuitBreakerTask.class);

        when(mock.processAndReturn()).thenThrow(new RuntimeException("test failure"));

        for (int i = 0; i < 11; i++) {
            circuitBreakerExecutor.execute(mock);
        }

        verify(mock, times(10)).processAndReturn();
        assertThat(circuitBreakerExecutor.getCurrentNumberOfErrors()).isEqualTo(10);
    }

    @Test
    public void closedAfterTenSeconds() {
        circuitBreakerExecutor.setOpenAfterErrorsThreshold(10);
        circuitBreakerExecutor.setRetryAfterSeconds(30);

        CircuitBreakerTask<String> mock = mock(CircuitBreakerTask.class);

        when(mock.processAndReturn()).thenThrow(new RuntimeException("test failure"));

        for (int i = 0; i < 11; i++) {
            circuitBreakerExecutor.execute(mock);
        }

        reset(clock, mock);
        Instant plusSeconds = start.plusSeconds(31);
        when(clock.instant()).thenReturn(plusSeconds);
        when(mock.processAndReturn()).thenReturn("success");
        when(mock.getFallback()).thenReturn("fallback");

        assertThat(circuitBreakerExecutor.execute(mock)).isEqualTo("success");
        assertThat(circuitBreakerExecutor.execute(mock)).isEqualTo("success");
        assertThat(circuitBreakerExecutor.execute(mock)).isEqualTo("success");
        assertThat(circuitBreakerExecutor.execute(mock)).isEqualTo("success");
    }

}
