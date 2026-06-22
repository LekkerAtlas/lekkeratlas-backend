package nl.lekkeratlas.shared.model.content;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import io.github.david.auk.fluid.jdbc.annotations.table.TableName;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableConstructor;
import io.github.david.auk.fluid.jdbc.annotations.table.field.PrimaryKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.TableColumn;
import io.github.david.auk.fluid.jdbc.components.tables.TableEntity;

@TableName("content")
public final class Content implements TableEntity {
        @TableColumn
        @PrimaryKey
        private final UUID id;
        @TableColumn(columnName = "content_type")
        private final ContentType type;
        @TableColumn
        private String title;
        @TableColumn
        private final String description;
        @TableColumn(columnName = "show_games_played_by_default")
        private final Boolean showGamesPlayedByDefault;
        @TableColumn(columnName = "original_published_at")
        private final Instant publishedAt;
        @TableColumn(columnName = "created_at")
        private final Instant createdAt;
        @TableColumn(columnName = "updated_at")
        private final Instant updatedAt;

        @SuppressWarnings("java:S107")
        @TableConstructor
        public Content(UUID id, ContentType type, String title, String description, Boolean showGamesPlayedByDefault,
                        Instant publishedAt, Instant createdAt, Instant updatedAt) {
                this.id = id;
                this.type = type;
                this.title = title;
                this.description = description;
                this.showGamesPlayedByDefault = showGamesPlayedByDefault;
                this.publishedAt = publishedAt;
                this.createdAt = createdAt;
                this.updatedAt = updatedAt;
        }

        public UUID id() {
                return id;
        }

        public ContentType type() {
                return type;
        }

        public String title() {
                return title;
        }

        public String description() {
                return description;
        }

        public Boolean showGamesPlayedByDefault() {
                return showGamesPlayedByDefault;
        }

        public Instant publishedAt() {
                return publishedAt;
        }

        public Instant createdAt() {
                return createdAt;
        }

        public Instant updatedAt() {
                return updatedAt;
        }

        @Override
        public boolean equals(Object obj) {
                if (obj == this)
                        return true;
                if (obj == null || obj.getClass() != this.getClass())
                        return false;
                var that = (Content) obj;
                return Objects.equals(this.id, that.id) &&
                                Objects.equals(this.type, that.type) &&
                                Objects.equals(this.title, that.title) &&
                                Objects.equals(this.description, that.description) &&
                                Objects.equals(this.showGamesPlayedByDefault, that.showGamesPlayedByDefault) &&
                                Objects.equals(this.publishedAt, that.publishedAt) &&
                                Objects.equals(this.createdAt, that.createdAt) &&
                                Objects.equals(this.updatedAt, that.updatedAt);
        }

        @Override
        public int hashCode() {
                return Objects.hash(id, type, title, description, showGamesPlayedByDefault, publishedAt, createdAt,
                                updatedAt);
        }

        @Override
        public String toString() {
                return "Content[" +
                                "id=" + id + ", " +
                                "type=" + type + ", " +
                                "title=" + title + ", " +
                                "description=" + description + ", " +
                                "showGamesPlayedByDefault=" + showGamesPlayedByDefault + ", " +
                                "publishedAt=" + publishedAt + ", " +
                                "createdAt=" + createdAt + ", " +
                                "updatedAt=" + updatedAt + ']';
        }

        public void setTitle(String title) {
                this.title = title;
        }
}
