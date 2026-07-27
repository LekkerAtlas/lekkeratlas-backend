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

@TableName("creator_account")
public final class CreatorAccount implements TableEntity {

        @PrimaryKey
        @TableColumn
        private final UUID id;

        @ForeignKey
        @TableColumn(columnName = "creator_id")
        private final Creator creator;

        @TableColumn(columnName = "account_kind")
        private final CreatorAccountKind accountKind;

        @TableColumn(columnName = "external_account_id")
        private final String externalAccountId;

        @TableColumn(columnName = "display_name")
        private final String displayName;

        @TableColumn(columnName = "fetch_new_content_is_automated")
        private final Boolean fetchNewContentIsAutomated;

        @ForeignKey
        @TableColumn(columnName = "added_by_user_id")
        private final User addedBy;

        @TableColumn(columnName = "created_at")
        private final Instant createdAt;

        @TableColumn(columnName = "updated_at")
        private final Instant updatedAt;

        @SuppressWarnings("java:S107")
        @TableConstructor
        public CreatorAccount(
                        UUID id,
                        Creator creator,
                        CreatorAccountKind accountKind,
                        String externalAccountId,
                        String displayName,
                        Boolean fetchNewContentIsAutomated,
                        User addedBy,
                        Instant createdAt,
                        Instant updatedAt) {

                this.id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
                this.creator = creator;
                this.accountKind = accountKind;
                this.externalAccountId = externalAccountId;
                this.displayName = displayName;
                this.fetchNewContentIsAutomated = fetchNewContentIsAutomated;
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

        public Creator getCreator() {
                return creator;
        }

        public CreatorAccountKind getAccountKind() {
                return accountKind;
        }

        public String getExternalAccountId() {
                return externalAccountId;
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

        public Instant getUpdatedAt() {
                return updatedAt;
        }

        public static CreatorAccount getDummyCreatorAccount(UUID id) {
                return new CreatorAccount(
                                id,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null);
        }
}
