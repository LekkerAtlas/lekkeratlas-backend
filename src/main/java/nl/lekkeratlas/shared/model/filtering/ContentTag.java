package nl.lekkeratlas.shared.model.filtering;

import io.github.david.auk.fluid.jdbc.annotations.table.TableName;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableConstructor;
import io.github.david.auk.fluid.jdbc.annotations.table.field.ForeignKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.PrimaryKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.TableColumn;
import nl.lekkeratlas.shared.model.content.Content;

@TableName("content_tag")
public class ContentTag {
        @ForeignKey
        @TableColumn(columnName = "content_id")
        private final Content content;

        @ForeignKey
        @TableColumn(columnName = "tag_id")
        private final Tag tag;

        @TableConstructor
        public ContentTag(Content content, Tag tag) {
                this.content = content;
                this.tag = tag;
        }

        /**
         * Primary key for the table.
         *
         * @return the conjoined content and tag id's
         */
        @PrimaryKey
        public String getPrimaryKey() {
                return content.id().toString() + tag.toString();
        }
}
