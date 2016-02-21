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


public class CircuitBreakerStressTest {

    @Test
    public void underPressure() throws Exception {
        CircuitBreakerExecutor withSystemClock = new CircuitBreakerExecutor();

        for (int i = 0; i < 100; i++) {
            List<Thread> threads = Lists.newArrayList();
            CountDownLatch signal = new CountDownLatch(1);

            int i1 = RandomUtils.nextInt(50, 1000);
            for (int j = 0; j < i1; j++) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            TestCircuitBreakerTask task = new TestCircuitBreakerTask();
                            signal.await();
                            Thread.sleep(RandomUtils.nextLong(1, 20));
                            withSystemClock.execute(task);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                    }
                };
                Thread thread = new Thread(runnable);
                threads.add(thread);
                thread.start();
            }

            signal.countDown();

            threads.forEach(t -> {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

    }

    private static class TestCircuitBreakerTask implements CircuitBreakerTask<String> {

        private static List<Boolean> FAILS = Lists.newArrayList(true, false, true, false, false);
        private boolean fail;

        private TestCircuitBreakerTask() {
            fail = FAILS.get(RandomUtils.nextInt(0, FAILS.size()));
        }

        @Override
        public String processAndReturn() {
            if (fail) {
                throw new RuntimeException("fail");
            }
            return "success";
        }

        @Override
        public String getFallback() {
            return "fallback";
        }
    }
}
