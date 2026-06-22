package nl.lekkeratlas.shared.model.content.hostedcontent;

import java.util.UUID;

import io.github.david.auk.fluid.jdbc.annotations.table.TableName;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableConstructor;
import io.github.david.auk.fluid.jdbc.annotations.table.field.ForeignKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.PrimaryKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.TableColumn;
import io.github.david.auk.fluid.jdbc.components.tables.TableEntity;
import nl.lekkeratlas.shared.model.content.Content;
import nl.lekkeratlas.shared.model.content.contentplatform.ContentPlatform;

@TableName("hosted_content")
public record HostedContent(@PrimaryKey @TableColumn UUID id,
                @ForeignKey @TableColumn(columnName = "content_id") Content content,
                @ForeignKey @TableColumn(columnName = "content_platform_id") ContentPlatform contentPlatform,
                @TableColumn(columnName = "external_content_id") String externalContentId) implements TableEntity {

        @TableConstructor
        public HostedContent {
                // So F
        }

        @Override
        public UUID id() {
                return id;
        }
}
