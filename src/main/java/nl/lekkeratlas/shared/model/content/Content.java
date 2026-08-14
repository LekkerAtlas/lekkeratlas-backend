package nl.lekkeratlas.shared.model.content;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import io.github.david.auk.fluid.jdbc.annotations.table.TableName;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableConstructor;
import io.github.david.auk.fluid.jdbc.annotations.table.field.ForeignKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.PrimaryKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.TableColumn;
import io.github.david.auk.fluid.jdbc.components.tables.TableEntity;

import nl.lekkeratlas.shared.model.content.creator.Creator;

@TableName("content")
public final class Content implements TableEntity {
        @TableColumn
        @PrimaryKey
        private final UUID id;
        @ForeignKey
        @TableColumn(columnName = "creator_id")
        private final Creator creator;
        @TableColumn(columnName = "content_type")
        private final ContentType type;
        @TableColumn
        private String title;
        @TableColumn(columnName = "duration_seconds")
        private final Integer durationSeconds;
        @TableColumn
        private final String description;
        @TableColumn(columnName = "show_games_played_by_default")
        private final Boolean showGamesPlayedByDefault;
        @TableColumn(columnName = "published_at")
        private final Instant publishedAt;
        @TableColumn(columnName = "is_behind_paywall")
        private final Boolean isBehindPaywall;
        @TableColumn(columnName = "created_at")
        private final Instant createdAt;
        @TableColumn(columnName = "updated_at")
        private final Instant updatedAt;

        @SuppressWarnings("java:S107")
        @TableConstructor
        public Content(UUID id, Creator creator, ContentType type, String title, Integer durationSeconds,
                        String description,
                        Boolean showGamesPlayedByDefault, Instant publishedAt, Boolean isBehindPaywall,
                        Instant createdAt, Instant updatedAt) {
                this.id = id;
                this.creator = creator;
                this.type = type;
                this.title = title;
                this.durationSeconds = durationSeconds;
                this.description = description;
                this.showGamesPlayedByDefault = showGamesPlayedByDefault;
                this.publishedAt = publishedAt;
                this.isBehindPaywall = isBehindPaywall;
                this.createdAt = createdAt;
                this.updatedAt = updatedAt;
        }

        public void setTitle(String title) {
                this.title = title;
        }

        public UUID getId() {
                return id;
        }

        public Creator getCreator() {
                return creator;
        }

        public ContentType getType() {
                return type;
        }

        public String getTitle() {
                return title;
        }

        public Integer getDurationSeconds() {
                return durationSeconds;
        }

        public String getDescription() {
                return description;
        }

        public Boolean getShowGamesPlayedByDefault() {
                return showGamesPlayedByDefault;
        }

        public Instant getPublishedAt() {
                return publishedAt;
        }

        public Boolean getIsBehindPaywall() {
                return isBehindPaywall;
        }

        public Instant getCreatedAt() {
                return createdAt;
        }

        public Instant getUpdatedAt() {
                return updatedAt;
        }

        @Override
        public boolean equals(Object obj) {
                if (obj == this)
                        return true;
                if (obj == null || obj.getClass() != this.getClass())
                        return false;
                var that = (Content) obj;
                return Objects.equals(this.id, that.id) && Objects.equals(this.creator, that.creator)
                                && Objects.equals(this.type, that.type) && Objects.equals(this.title, that.title)
                                && Objects.equals(this.description, that.description)
                                && Objects.equals(this.showGamesPlayedByDefault, that.showGamesPlayedByDefault)
                                && Objects.equals(this.publishedAt, that.publishedAt)
                                && Objects.equals(this.createdAt, that.createdAt)
                                && Objects.equals(this.updatedAt, that.updatedAt);
        }

        @Override
        public int hashCode() {
                return Objects.hash(id, creator, type, title, description, showGamesPlayedByDefault, publishedAt,
                                createdAt, updatedAt);
        }

        @Override
        public String toString() {
                return "Content [id=" + id + ", creator=" + creator + ", type=" + type + ", title=" + title
                                + ", durationSeconds=" + durationSeconds + ", description=" + description
                                + ", showGamesPlayedByDefault=" + showGamesPlayedByDefault + ", publishedAt="
                                + publishedAt + ", isBehindPaywall=" + isBehindPaywall + ", createdAt=" + createdAt
                                + ", updatedAt=" + updatedAt + "]";
        }

}
