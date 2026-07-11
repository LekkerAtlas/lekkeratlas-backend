package nl.lekkeratlas.worker.cancellation;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.github.david.auk.fluid.jdbc.components.Database;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.rabbit.WorkCommandEventSync;

@Component
public class QueueJobCancellationMonitor {

        private static final Logger logger = LoggerFactory.getLogger(
                        QueueJobCancellationMonitor.class);

        private final ScheduledExecutorService scheduler;
        private final WorkCommandEventSync workCommandEventSync;

        public QueueJobCancellationMonitor(
                        @Qualifier("queueJobCancellationScheduler") ScheduledExecutorService scheduler,
                        WorkCommandEventSync workCommandEventSync) {

                this.scheduler = scheduler;
                this.workCommandEventSync = workCommandEventSync;
        }

        public QueueJobCancellationWatch watch(
                        QueueJob queueJob,
                        Runnable cancellationAction) {

                AtomicBoolean stopped = new AtomicBoolean(false);

                ScheduledFuture<?> pollingTask = scheduler.scheduleWithFixedDelay(
                                () -> poll(
                                                queueJob,
                                                cancellationAction,
                                                stopped),
                                0,
                                1,
                                TimeUnit.SECONDS);

                return () -> {
                        stopped.set(true);
                        pollingTask.cancel(false);
                };
        }

        private void poll(
                        QueueJob queueJob,
                        Runnable cancellationAction,
                        AtomicBoolean stopped) {

                if (stopped.get()) {
                        return;
                }

                try (Connection connection = Database.getConnection()) {
                        boolean canceled = workCommandEventSync.isCanceled(
                                        connection,
                                        queueJob);

                        if (canceled
                                        && stopped.compareAndSet(false, true)) {

                                logger.info(
                                                "Cancellation requested for queue job {}",
                                                queueJob.getId());

                                cancellationAction.run();
                        }
                } catch (SQLException | RuntimeException exception) {
                        logger.error(
                                        "Could not check cancellation status for queue job {}",
                                        queueJob.getId(),
                                        exception);
                }
        }
}
