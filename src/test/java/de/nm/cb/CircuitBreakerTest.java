package de.nm.cb;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerTest {

    private CircuitBreakerExecutor circuitBreakerExecutor;

    @Before
    public void setUp() {
        circuitBreakerExecutor = new CircuitBreakerExecutor();
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
    }

    @Test
    public void error() {
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
    }

}
