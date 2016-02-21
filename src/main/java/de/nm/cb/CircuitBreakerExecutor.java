package de.nm.cb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CircuitBreakerExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerExecutor.class);

    public static final int DEFAULT_OPEN_AFTER_ERRORS_THRESHOLD = 50;

    private int openAfterErrorsThreshold = DEFAULT_OPEN_AFTER_ERRORS_THRESHOLD;

    /**
     * clock for testing.
     */
    private Clock clock;

    /**
     * state.
     */
    private AtomicLong next;
    private AtomicBoolean open = new AtomicBoolean(false);

    /**
     * configuration.
     */
    private AtomicInteger numberOfErrors = new AtomicInteger(0);
    private AtomicInteger retryAfterSeconds = new AtomicInteger(5);

    public CircuitBreakerExecutor() {
        this(Clock.systemDefaultZone());
    }

    public CircuitBreakerExecutor(Clock clock) {
        this.clock = clock;
        this.next = new AtomicLong(clock.instant().toEpochMilli());
    }

    public <T> T execute(CircuitBreakerTask<T> circuitBreakerTask) {
        Objects.requireNonNull(circuitBreakerTask, "task must not be null");
        String name = circuitBreakerTask.getClass().getName();

        LOG.trace("Start executing {}", circuitBreakerTask.getClass().getName());

        switchToOpenIfClosedAndErrorsLimitReached(circuitBreakerTask);

        Requester<T> tRequester = new Requester<>(circuitBreakerTask);
        Response<T> tResponse;

        if (isOpen() && nextCheckOfOpenCircuitReached()) {
            LOG.info("Check for closing Circuit::{} after {}", name, clock.instant());
            tResponse = tRequester.processWithFallbackOnError(handleOpenPeriodReachedCallback(name));
        } else if (isOpen()) {
            LOG.trace("Circuit::{} is open. Returning fallback.", name);
            tResponse = tRequester.fallback();
        } else {
            tResponse = tRequester.processWithFallbackOnError(defaultCallback(name));
        }

        return tResponse.getResult();
    }

    private RequestCallback defaultCallback(String name) {
        return new RequestCallback() {
            @Override
            public void onSuccess() {
                LOG.info("Circuit::{} Success {}", name, numberOfErrors);
                countDownNumberOfErrors();
            }

            @Override
            public void onError(Throwable throwable) {
                LOG.info("Circuit::{} Error {}", name, numberOfErrors);
                registerNewError();
            }
        };
    }

    private void countDownNumberOfErrors() {
        if (numberOfErrors.get() > 0) {
            numberOfErrors.set(numberOfErrors.get() - 1);
        }
    }

    private RequestCallback handleOpenPeriodReachedCallback(String name) {
        return new RequestCallback() {
            @Override
            public void onSuccess() {
                LOG.info("Circuit::{} Success {}", name, numberOfErrors);
                closeCircuit();
            }

            @Override
            public void onError(Throwable throwable) {
                LOG.info("Circuit::{} Error {}", name, numberOfErrors);
                startOpenPeriod();
                registerNewError();
            }
        };
    }

    private void registerNewError() {numberOfErrors.incrementAndGet();}

    private boolean nextCheckOfOpenCircuitReached() {
        return clock.instant().toEpochMilli() >= next.get();
    }

    private void closeCircuit() {
        open.set(false);
        countDownNumberOfErrors();
    }

    public boolean isOpen() {return open.get();}

    private void switchToOpenIfClosedAndErrorsLimitReached(CircuitBreakerTask circuitBreakerTask) {
        if (isClosed() && numberOfErrors.get() >= openAfterErrorsThreshold) {
            openCircuit();
            startOpenPeriod();
            String name = circuitBreakerTask.getClass().getName();
            Instant instant = Instant.ofEpochMilli(next.get());
            LOG.info("Open Circuit::{} after {} errors at {} wait until {}",
                    name, numberOfErrors, instant, instant.plusSeconds(retryAfterSeconds.get()));
        }
    }

    private void startOpenPeriod() {
        this.next.set(this.clock.instant().plusSeconds(retryAfterSeconds.get()).toEpochMilli());
    }

    private void openCircuit() {
        open.getAndSet(true);
    }

    private boolean isClosed() {return !isOpen();}

    public void setOpenAfterErrorsThreshold(int openAfterErrorsThreshold) {
        if (isLessOrEqZero(openAfterErrorsThreshold)) {
            openAfterErrorsThreshold = DEFAULT_OPEN_AFTER_ERRORS_THRESHOLD;
        }
        this.openAfterErrorsThreshold = openAfterErrorsThreshold;
    }

    public int getCurrentNumberOfErrors() {
        return numberOfErrors.get();
    }

    private boolean isLessOrEqZero(int openAfterErrorsThreshold) {return openAfterErrorsThreshold <= 0;}

    public void setRetryAfterSeconds(int retryAfterSeconds) {
        this.retryAfterSeconds.set(retryAfterSeconds);
    }
}
