package nl.lekkeratlas.backendapi.exceptions;

import nl.lekkeratlas.shared.model.queue.QueueJob;

public class JobAlreadyFinishedException extends QueueJobException {

        public JobAlreadyFinishedException(QueueJob queueJob) {
                super("Illegal operation on finished job " + queueJob.getId());
        }
}
