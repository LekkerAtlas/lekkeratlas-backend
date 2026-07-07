package nl.lekkeratlas.worker.cancellation;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import io.github.david.auk.fluid.jdbc.components.Database;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.rabbit.WorkCommandEventSync;
import nl.lekkeratlas.worker.exceptions.CanceledQueueJobException;

@Component
public class QueueJobCancellationMonitor {

        private static final Logger logger = LoggerFactory.getLogger(QueueJobCancellationMonitor.class);

        private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);

        private final TaskScheduler scheduler;
        private final WorkCommandEventSync workCommandEventSync;

        public QueueJobCancellationMonitor(
                        @Qualifier("queueJobCancellationScheduler") TaskScheduler scheduler,
                        WorkCommandEventSync workCommandEventSync) {

                this.scheduler = scheduler;
                this.workCommandEventSync = workCommandEventSync;
        }

        public QueueJobCancellationWatch watch(QueueJob queueJob) {
                PollingCancellationWatch watch = new PollingCancellationWatch(
                                queueJob,
                                Thread.currentThread());

                ScheduledFuture<?> pollingTask = scheduler.scheduleWithFixedDelay(
                                watch::poll,
                                POLL_INTERVAL);

                watch.setPollingTask(pollingTask);

                return watch;
        }

        private final class PollingCancellationWatch
                        implements QueueJobCancellationWatch {

                private final QueueJob queueJob;
                private final Thread workerThread;

                private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);

                private final AtomicBoolean closed = new AtomicBoolean(false);

                private volatile ScheduledFuture<?> pollingTask;

                private PollingCancellationWatch(
                                QueueJob queueJob,
                                Thread workerThread) {

                        this.queueJob = queueJob;
                        this.workerThread = workerThread;
                }

                private void setPollingTask(ScheduledFuture<?> pollingTask) {
                        this.pollingTask = pollingTask;

                        // Handle the unlikely race where the first poll completed
                        // before the ScheduledFuture was assigned.
                        if (closed.get() || cancellationRequested.get()) {
                                pollingTask.cancel(false);
                        }
                }

                private void poll() {
                        if (closed.get() || cancellationRequested.get()) {
                                cancelPolling();
                                return;
                        }

                        try (Connection connection = Database.getConnection()) {
                                boolean canceled = workCommandEventSync.isCanceled(
                                                connection,
                                                queueJob);

                                if (canceled
                                                && !closed.get()
                                                && cancellationRequested.compareAndSet(
                                                                false,
                                                                true)) {

                                        logger.info(
                                                        "Interrupting canceled queue job {}",
                                                        queueJob.getId());

                                        workerThread.interrupt();
                                        cancelPolling();
                                }
                        } catch (SQLException | RuntimeException exception) {
                                /*
                                 * Do not let an exception escape from a periodic task.
                                 * Otherwise the scheduler may stop future cancellation
                                 * checks for this job.
                                 */
                                logger.error(
                                                "Could not check cancellation status for queue job {}",
                                                queueJob.getId(),
                                                exception);
                        }
                }

                @Override
                public boolean isCancellationRequested() {
                        return cancellationRequested.get()
                                        || workerThread.isInterrupted();
                }

                @Override
                public void close() {
                        closed.set(true);
                        cancelPolling();
                }

                private void cancelPolling() {
                        ScheduledFuture<?> task = pollingTask;

                        if (task != null) {
                                task.cancel(false);
                        }
                }
        }
}
