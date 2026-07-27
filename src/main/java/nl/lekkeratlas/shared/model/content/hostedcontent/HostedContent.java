package nl.lekkeratlas.shared.model.content.hostedcontent;

import java.time.Instant;
import java.util.UUID;

import io.github.david.auk.fluid.jdbc.annotations.table.TableName;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableConstructor;
import io.github.david.auk.fluid.jdbc.annotations.table.field.ForeignKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.PrimaryKey;
import io.github.david.auk.fluid.jdbc.annotations.table.field.TableColumn;
import io.github.david.auk.fluid.jdbc.components.tables.TableEntity;
import nl.lekkeratlas.shared.model.content.Content;
import nl.lekkeratlas.shared.model.content.creator.Creator;
import nl.lekkeratlas.shared.model.content.creator.CreatorAccount;

@TableName("hosted_content")
public record HostedContent(
                @PrimaryKey @TableColumn UUID id,
                @ForeignKey @TableColumn(columnName = "content_id") Content content,
                @ForeignKey @TableColumn(columnName = "creator_id") Creator creator,
                @ForeignKey @TableColumn(columnName = "creator_account_id") CreatorAccount creatorAccount,
                @TableColumn(columnName = "external_content_id") String externalContentId,
                @TableColumn(columnName = "created_at") Instant createdAt) implements TableEntity {

        @TableConstructor
        public HostedContent {
                // Constructor used by fluid-jdbc and application code.
        }

        @Override
        public UUID id() {
                return id;
        }
}
