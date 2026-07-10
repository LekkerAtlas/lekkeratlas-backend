package nl.lekkeratlas.worker.execution;

import java.util.concurrent.atomic.AtomicBoolean;

import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.worker.exceptions.CanceledQueueJobException;

public final class QueueJobCancellationToken {

        private final QueueJob queueJob;

        private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);

        QueueJobCancellationToken(QueueJob queueJob) {
                this.queueJob = queueJob;
        }

        void requestCancellation() {
                cancellationRequested.set(true);
        }

        public boolean isCancellationRequested() {
                return cancellationRequested.get();
        }

        public void checkpoint(String message)
                        throws CanceledQueueJobException {

                if (cancellationRequested.get()) {
                        throw canceledException(message);
                }
        }

        public CanceledQueueJobException canceledException(
                        String message) {

                return new CanceledQueueJobException(
                                queueJob,
                                "User canceled the queue job",
                                message);
        }
}
