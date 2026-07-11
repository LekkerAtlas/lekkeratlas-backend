package nl.lekkeratlas.shared.model.queue;

import java.time.Instant;

import io.github.david.auk.fluid.jdbc.annotations.table.TableName;
import io.github.david.auk.fluid.jdbc.annotations.table.field.ForeignKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.PrimaryKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.TableColumn;
import io.github.david.auk.fluid.jdbc.components.tables.TableEntity;

@TableName("queue_job_cancellation_request")
public record QueueJobCancellationRequest(
                @PrimaryKey @ForeignKey @TableColumn(columnName = "job_id") QueueJob queueJob,
                @TableColumn(columnName = "requested_at") Instant requestedAt) implements TableEntity {
}
