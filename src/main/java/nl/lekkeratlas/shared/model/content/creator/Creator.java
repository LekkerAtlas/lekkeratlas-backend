package nl.lekkeratlas.shared.model.content.creator;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import io.github.david.auk.fluid.jdbc.annotations.table.TableName;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableConstructor;
import io.github.david.auk.fluid.jdbc.annotations.table.field.ForeignKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.PrimaryKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.TableColumn;
import io.github.david.auk.fluid.jdbc.components.tables.TableEntity;
import nl.lekkeratlas.shared.model.user.User;

@TableName("creator")
public final class Creator implements TableEntity {

        @PrimaryKey
        @TableColumn
        private final UUID id;

        @TableColumn(columnName = "display_name")
        private final String displayName;

        @ForeignKey
        @TableColumn(columnName = "added_by_user_id")
        private final User addedBy;

        @TableColumn(columnName = "created_at")
        private final Instant createdAt;

        @TableColumn(columnName = "updated_at")
        private final Instant updatedAt;

        @TableConstructor
        public Creator(
                        UUID id,
                        String displayName,
                        User addedBy,
                        Instant createdAt,
                        Instant updatedAt) {

                this.id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
                this.displayName = displayName;
                this.addedBy = addedBy;
                this.createdAt = createdAt;
                this.updatedAt = updatedAt;
        }

        public UUID id() {
                return id;
        }

        public UUID getId() {
                return id;
        }

        public String getDisplayName() {
                return displayName;
        }

        public User getAddedBy() {
                return addedBy;
        }

        public Instant getCreatedAt() {
                return createdAt;
        }

        public Instant getUpdatedAt() {
                return updatedAt;
        }

        public static Creator getDummyContentCreator(UUID id) {
                return new Creator(
                                id,
                                null,
                                null,
                                null,
                                null);
        }
}
