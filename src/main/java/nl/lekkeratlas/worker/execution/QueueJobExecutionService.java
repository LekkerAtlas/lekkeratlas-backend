package nl.lekkeratlas.worker.execution;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.worker.cancellation.QueueJobCancellationMonitor;
import nl.lekkeratlas.worker.cancellation.QueueJobCancellationWatch;

@Component
public class QueueJobExecutionService {

        private final ExecutorService executor;
        private final QueueJobCancellationMonitor cancellationMonitor;

        public QueueJobExecutionService(
                        @Qualifier("queueJobExecutor") ExecutorService executor,
                        QueueJobCancellationMonitor cancellationMonitor) {

                this.executor = executor;
                this.cancellationMonitor = cancellationMonitor;
        }

        public void executeAndWait(
                        QueueJob queueJob,
                        QueueJobTask task)
                        throws Exception {

                QueueJobCancellationToken cancellation = new QueueJobCancellationToken(queueJob);

                AtomicReference<Thread> workerThread = new AtomicReference<>();

                Future<Void> future = executor.submit(() -> {
                        workerThread.set(Thread.currentThread());

                        try {
                                cancellation.checkpoint(
                                                "Queue job was canceled before execution");

                                task.run(cancellation);

                                /*
                                 * Prevent a handler from marking itself COMPLETED after a
                                 * cancellation was registered near the end.
                                 */
                                cancellation.checkpoint(
                                                "Queue job was canceled before completion");

                                return null;
                        } catch (InterruptedException exception) {
                                if (cancellation.isCancellationRequested()) {
                                        throw cancellation.canceledException(
                                                        "Queue job execution was interrupted");
                                }

                                Thread.currentThread().interrupt();
                                throw exception;
                        } finally {
                                workerThread.set(null);

                                /*
                                 * This is a reusable executor thread. Ensure an interrupt from
                                 * one job does not leak into the next job.
                                 */
                                Thread.interrupted();
                        }
                });

                Runnable requestCancellation = () -> {
                        cancellation.requestCancellation();

                        Thread runningThread = workerThread.get();

                        if (runningThread != null) {
                                runningThread.interrupt();
                        }
                };

                try (QueueJobCancellationWatch ignored = cancellationMonitor.watch(
                                queueJob,
                                requestCancellation)) {

                        awaitCompletion(
                                        queueJob,
                                        future,
                                        requestCancellation);
                }
        }

        private void awaitCompletion(
                        QueueJob queueJob,
                        Future<Void> future,
                        Runnable requestCancellation)
                        throws Exception {

                try {
                        future.get();
                } catch (ExecutionException exception) {
                        rethrowCause(exception.getCause());
                } catch (InterruptedException exception) {
                        /*
                         * The Rabbit listener thread itself was interrupted, typically
                         * during container shutdown. Stop the job thread and requeue the
                         * Rabbit message.
                         */
                        requestCancellation.run();
                        Thread.currentThread().interrupt();

                        throw new ImmediateRequeueAmqpException(
                                        "Listener interrupted while waiting for queue job "
                                                        + queueJob.getId(),
                                        exception);
                }
        }

        private void rethrowCause(Throwable cause)
                        throws Exception {

                if (cause instanceof Exception exception) {
                        throw exception;
                }

                if (cause instanceof Error error) {
                        throw error;
                }

                throw new IllegalStateException(
                                "Queue job failed with an unknown throwable",
                                cause);
        }
}
