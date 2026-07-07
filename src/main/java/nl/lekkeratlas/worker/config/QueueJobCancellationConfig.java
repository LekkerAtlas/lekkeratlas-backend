package nl.lekkeratlas.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class QueueJobCancellationConfig {

        @Bean("queueJobCancellationScheduler")
        TaskScheduler queueJobCancellationScheduler() {
                ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

                scheduler.setPoolSize(2);
                scheduler.setThreadNamePrefix("queue-cancellation-");

                /*
                 * Remove canceled polling tasks from the underlying executor queue.
                 */
                scheduler.setRemoveOnCancelPolicy(true);

                /*
                 * Do not execute pending cancellation polls while the application
                 * is shutting down.
                 */
                scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

                return scheduler;
        }
}
