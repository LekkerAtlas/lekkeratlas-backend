package nl.lekkeratlas.worker.exceptions;

import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobStatus;

public class CanceledQueueJobException extends QueueJobException {

        public CanceledQueueJobException(QueueJob queueJob, String message, String messageForEndUser) {
                super(queueJob, message, messageForEndUser, QueueJobStatus.CANCELED);
        }
}
