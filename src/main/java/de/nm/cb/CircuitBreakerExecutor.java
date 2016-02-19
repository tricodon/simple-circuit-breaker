package de.nm.cb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CircuitBreakerExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerExecutor.class);

    public <T> T execute(CircuitBreakerTask<T> circuitBreakerTask) {
        Check.notNull(circuitBreakerTask, "circuitBreakerTask must not be null");

        LOG.trace("Start executing {}", circuitBreakerTask.getClass().getName());

        try {
            return circuitBreakerTask.processAndReturn();
        } catch (Throwable onProcessException) {

            try {
                return circuitBreakerTask.getFallback();
            } catch (Exception fallbackException) {
                throw new RuntimeException(fallbackException);
            }

        }
    }
}
