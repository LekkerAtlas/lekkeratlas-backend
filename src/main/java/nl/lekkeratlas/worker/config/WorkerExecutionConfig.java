package nl.lekkeratlas.worker.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerExecutionConfig {

        @Bean(name = "queueJobExecutor", destroyMethod = "shutdown")
        ExecutorService queueJobExecutor() {
                return Executors.newFixedThreadPool(
                                1,
                                namedThreadFactory("queue-job-worker-", false));
        }

        @Bean(name = "queueJobCancellationScheduler", destroyMethod = "shutdown")
        ScheduledExecutorService queueJobCancellationScheduler() {
                return Executors.newScheduledThreadPool(
                                2,
                                namedThreadFactory("queue-job-cancellation-", true));
        }

        private ThreadFactory namedThreadFactory(
                        String prefix,
                        boolean daemon) {

                AtomicInteger sequence = new AtomicInteger();

                return runnable -> {
                        Thread thread = new Thread(
                                        runnable,
                                        prefix + sequence.incrementAndGet());

                        thread.setDaemon(daemon);

                        return thread;
                };
        }
}
