package nl.lekkeratlas.worker.exceptions;

import nl.lekkeratlas.shared.model.queue.QueueJob;

public class FailedQueueJobException extends QueueJobException {

        public FailedQueueJobException(QueueJob queueJob, String message, String messageForEndUser) {
                super(queueJob, message, messageForEndUser);
        }

        public FailedQueueJobException(QueueJob queueJob, Exception exception) {
                super(queueJob, exception.getMessage(), "There was an internal server error, " +
                                "please send this to an administrator: " + exception.getMessage());
        }
}
