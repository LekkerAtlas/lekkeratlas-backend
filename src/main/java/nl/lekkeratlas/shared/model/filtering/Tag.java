package nl.lekkeratlas.shared.model.filtering;

import java.time.Instant;
import java.util.UUID;

import io.github.david.auk.fluid.jdbc.annotations.table.TableName;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableConstructor;
import io.github.david.auk.fluid.jdbc.annotations.table.field.PrimaryKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.TableColumn;
import io.github.david.auk.fluid.jdbc.annotations.table.field.UniqueColumn;

@TableName("tag")
public class Tag {
        @PrimaryKey
        @TableColumn
        private final UUID id;

        @TableColumn
        @UniqueColumn
        private final String name;

        @TableColumn(columnName = "created_at")
        private final Instant createdAt;

        @TableConstructor
        public Tag(UUID id, String name, Instant createdAt) {
                this.id = id;
                this.name = name;
                this.createdAt = createdAt;
        }

        public UUID getId() {
                return id;
        }

        public String getName() {
                return name;
        }

        public Instant getCreatedAt() {
                return createdAt;
        }
}
