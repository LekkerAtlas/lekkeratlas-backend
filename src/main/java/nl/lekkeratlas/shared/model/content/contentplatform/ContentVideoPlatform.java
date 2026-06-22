package nl.lekkeratlas.shared.model.content.contentplatform;

import io.github.david.auk.fluid.jdbc.annotations.table.TableName;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableConstructor;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableInherits;
import io.github.david.auk.fluid.jdbc.annotations.table.field.TableColumn;

@TableName("content_video_platform")
@TableInherits(ContentPlatform.class)
public class ContentVideoPlatform extends ContentPlatform {

        /**
         * This is how we enforce abstraction implementation on a DB level.
         */
        @TableColumn(columnName = "platform_kind")
        private final ContentPlatformKind platformKind;

        @TableColumn(columnName = "source_kind")
        private final SourceKind sourceKind;

        @TableConstructor
        public ContentVideoPlatform(ContentPlatform contentPlatform, ContentPlatformKind platformKind,
                        SourceKind sourceKind) {
                super(contentPlatform);
                if (platformKind != ContentPlatformKind.VIDEO) {
                        throw new IllegalArgumentException("ContentPlatformKind must be VIDEO");
                }
                this.platformKind = platformKind;
                this.sourceKind = sourceKind;
        }

        public ContentVideoPlatform(ContentPlatform contentPlatform, SourceKind sourceKind) {
                super(contentPlatform);
                this.platformKind = ContentPlatformKind.VIDEO;
                this.sourceKind = sourceKind;
        }

        public ContentVideoPlatform(ContentVideoPlatform contentVideoPlatform) {
                super(contentVideoPlatform);
                this.platformKind = contentVideoPlatform.platformKind;
                this.sourceKind = contentVideoPlatform.sourceKind;
        }

        @Override
        public ContentPlatformKind getPlatformKind() {
                return platformKind;
        }
}
