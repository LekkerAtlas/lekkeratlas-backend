package nl.lekkeratlas.worker.exceptions;

import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobStatus;

public abstract class QueueJobException extends Exception {

        private final String messageForEndUser;
        private final QueueJob queueJob;
        private final QueueJobStatus status;


        QueueJobException(QueueJob queueJob, String message, String messageForEndUser, QueueJobStatus status) {
                super(message);
                this.messageForEndUser = messageForEndUser;
                this.queueJob = queueJob;

                if (!status.isStopped()) {
                        throw new IllegalArgumentException("Status must be stopped and not " + status);
                }

                this.status = status;
        }

        QueueJobException(QueueJob queueJob, String message, String messageForEndUser) {
                this(queueJob, message, messageForEndUser, QueueJobStatus.FAILED);
        }

        public QueueJob getQueueJob() {
                return queueJob;
        }

        public String getMessageForEndUser() {
                return messageForEndUser;
        }

        public QueueJobStatus getStatus() {
                return status;
        }
}
