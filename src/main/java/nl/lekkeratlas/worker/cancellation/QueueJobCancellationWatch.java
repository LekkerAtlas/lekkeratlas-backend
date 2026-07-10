package nl.lekkeratlas.worker.cancellation;

@FunctionalInterface
public interface QueueJobCancellationWatch extends AutoCloseable {

        @Override
        void close();
}
