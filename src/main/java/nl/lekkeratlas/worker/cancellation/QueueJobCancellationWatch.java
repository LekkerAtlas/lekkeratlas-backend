package nl.lekkeratlas.worker.cancellation;

public interface QueueJobCancellationWatch extends AutoCloseable {

        boolean isCancellationRequested();

        @Override
        void close();
}
