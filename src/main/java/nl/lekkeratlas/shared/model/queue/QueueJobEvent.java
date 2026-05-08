package nl.lekkeratlas.shared.model.queue;

import io.github.david.auk.fluid.jdbc.annotations.table.TableName;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableConstructor;
import io.github.david.auk.fluid.jdbc.annotations.table.field.ForeignKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.PrimaryKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.TableColumn;
import io.github.david.auk.fluid.jdbc.components.tables.TableEntity;

import java.time.Instant;
import java.util.UUID;

@TableName("queue_job_event")
public class QueueJobEvent implements TableEntity {

    @PrimaryKey
    @TableColumn
    private final UUID id;

    @ForeignKey
    @TableColumn(columnName = "job_id")
    private final QueueJob job;

    @TableColumn
    private final QueueJobStatus status;

    @TableColumn
    private final String message;

    @TableColumn(columnName = "created_at")
    private final Instant createdAt;

    @TableConstructor
    public QueueJobEvent(UUID id, QueueJob job, QueueJobStatus status, String message, Instant createdAt) {
        this.id = id;
        this.job = job;
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public QueueJob getJob() {
        return job;
    }

    public QueueJobStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
