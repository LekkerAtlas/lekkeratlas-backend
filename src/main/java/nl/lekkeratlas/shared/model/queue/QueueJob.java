package nl.lekkeratlas.shared.model.queue;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import io.github.david.auk.fluid.jdbc.annotations.table.TableName;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableConstructor;
import io.github.david.auk.fluid.jdbc.annotations.table.field.ForeignKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.PrimaryKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.TableColumn;
import io.github.david.auk.fluid.jdbc.components.tables.TableEntity;
import nl.lekkeratlas.shared.model.user.User;

@TableName("queue_job")
public class QueueJob implements TableEntity, Serializable {
        @PrimaryKey
        @TableColumn
        private final UUID id;

        @ForeignKey
        @TableColumn(columnName = "parent_job_id")
        private final QueueJob parentJob;

        @TableColumn
        private final QueueJobType type;

        @TableColumn
        private final QueueJobStatus status; // Default to "queued"

        @TableColumn
        private final Map<String, Serializable> payload;

        /**
         * Do not serialize the user for privacy sake
         * <p>
         * Make User serialize if this is restrictive
         */
        @ForeignKey
        @TableColumn(columnName = "requested_by_user_id")
        private final transient User requestedBy;

        @TableColumn(columnName = "correlation_key")
        private final String correlationKey;

        @TableColumn(columnName = "dedupe_key")
        private final String dedupeKey;

        @TableColumn(columnName = "error_type")
        private final String errorType;

        @TableColumn(columnName = "error_message")
        private final String errorMessage;

        @TableColumn(columnName = "created_at")
        private final Instant createdAt;

        @TableColumn(columnName = "started_at")
        private final Instant startedAt;

        @TableColumn(columnName = "finished_at")
        private final Instant finishedAt;

        @SuppressWarnings("java:S107")
        @TableConstructor
        public QueueJob(UUID id, QueueJob parentJob, QueueJobType type, QueueJobStatus status,
                        Map<String, Serializable> payload, User requestedBy, String correlationKey, String dedupeKey,
                        String errorType, String errorMessage, Instant createdAt, Instant startedAt,
                        Instant finishedAt) {
                this.id = id;
                this.parentJob = parentJob;
                this.type = type;
                this.status = status;
                this.payload = payload;
                this.requestedBy = requestedBy;
                this.correlationKey = correlationKey;
                this.dedupeKey = dedupeKey;
                this.errorType = errorType;
                this.errorMessage = errorMessage;
                this.createdAt = createdAt;
                this.startedAt = startedAt;
                this.finishedAt = finishedAt;
        }

        public UUID getId() {
                return id;
        }

        public QueueJob getParentJob() {
                return parentJob;
        }

        public QueueJobType getType() {
                return type;
        }

        public QueueJobStatus getStatus() {
                return status;
        }

        public Map<String, Serializable> getPayload() {
                return payload;
        }

        public User getRequestedBy() {
                return requestedBy;
        }

        public String getCorrelationKey() {
                return correlationKey;
        }

        public String getDedupeKey() {
                return dedupeKey;
        }

        public String getErrorType() {
                return errorType;
        }

        public String getErrorMessage() {
                return errorMessage;
        }

        public Instant getCreatedAt() {
                return createdAt;
        }

        public Instant getStartedAt() {
                return startedAt;
        }

        public Instant getFinishedAt() {
                return finishedAt;
        }
}
