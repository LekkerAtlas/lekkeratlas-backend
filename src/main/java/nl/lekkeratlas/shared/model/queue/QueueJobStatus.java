package nl.lekkeratlas.shared.model.queue;

import io.github.david.auk.fluid.jdbc.annotations.enums.EnumFormat;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Queue job status.
        
        - `QUEUED` — row exists and message should be processed
        - `RUNNING` — worker picked it up
        - `COMPLETED` — work succeeded
        - `FAILED` — work failed permanently
        - `CANCELED` — user/system canceled it
        """)

@EnumFormat(db = EnumFormat.Strategy.lower_snake_case, local = EnumFormat.Strategy.UPPER_SNAKE_CASE)
public enum QueueJobStatus {
        QUEUED,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELED;

        public boolean isActive() {
                return this == QUEUED || this == RUNNING;
        }

        public boolean isFinished() {
                return this == COMPLETED || this == FAILED || this == CANCELED;
        }

        public boolean isStopped() {
                return this == CANCELED || this == FAILED;
        }
}