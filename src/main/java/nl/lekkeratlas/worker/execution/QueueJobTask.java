package nl.lekkeratlas.worker.execution;

@FunctionalInterface
public interface QueueJobTask {

        void run(QueueJobCancellationToken cancellation)
                        throws Exception;
}
