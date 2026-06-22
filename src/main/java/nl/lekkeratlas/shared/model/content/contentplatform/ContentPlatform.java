package nl.lekkeratlas.shared.model.content.contentplatform;

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

@TableName("content_platform")
public class ContentPlatform implements TableEntity {

        @PrimaryKey
        @TableColumn
        private final UUID id;

        @TableColumn(columnName = "platform_kind")
        private final ContentPlatformKind platformKind;

        @TableColumn(columnName = "display_name")
        private final String displayName;

        @TableColumn(columnName = "fetch_new_content_is_automated")
        private final Boolean fetchNewContentIsAutomated; // TODO think about interval value

        @ForeignKey
        @TableColumn(columnName = "added_by_user_id")
        private final User addedBy;

        @TableColumn(columnName = "created_at")
        private final Instant createdAt;

        @TableConstructor
        public ContentPlatform(UUID id, ContentPlatformKind platformKind, String displayName,
                        Boolean fetchNewContentIsAutomated, User addedBy, Instant createdAt) {
                this.id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
                this.platformKind = platformKind;
                this.displayName = displayName;
                this.fetchNewContentIsAutomated = fetchNewContentIsAutomated;
                this.addedBy = addedBy;
                this.createdAt = createdAt;
        }

        public ContentPlatform(ContentPlatform contentPlatform) {
                this(
                                contentPlatform.getId(),
                                contentPlatform.getPlatformKind(),
                                contentPlatform.getDisplayName(),
                                contentPlatform.getFetchNewContentIsAutomated(),
                                contentPlatform.getAddedBy(),
                                contentPlatform.getCreatedAt());
        }

        public UUID getId() {
                return id;
        }

        public ContentPlatformKind getPlatformKind() {
                return platformKind;
        }

        public String getDisplayName() {
                return displayName;
        }

        public Boolean getFetchNewContentIsAutomated() {
                return fetchNewContentIsAutomated;
        }

        public User getAddedBy() {
                return addedBy;
        }

        public Instant getCreatedAt() {
                return createdAt;
        }

        public static ContentPlatform getDummyContentPlatform(UUID id) {
                return new ContentPlatform(
                                id,
                                null,
                                null,
                                null,
                                null,
                                null);
        }
}
