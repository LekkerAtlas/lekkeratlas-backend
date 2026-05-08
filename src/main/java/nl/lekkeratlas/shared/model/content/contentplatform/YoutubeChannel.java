package nl.lekkeratlas.shared.model.content.contentplatform;

import io.github.david.auk.fluid.jdbc.annotations.table.TableName;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableConstructor;
import io.github.david.auk.fluid.jdbc.annotations.table.constructor.TableInherits;
import io.github.david.auk.fluid.jdbc.annotations.table.field.TableColumn;
import io.github.david.auk.fluid.jdbc.components.tables.TableEntity;

@TableName("youtube_channel")
@TableInherits(ContentVideoPlatform.class)
public class YoutubeChannel extends ContentVideoPlatform implements TableEntity {

        @TableColumn(columnName = "source_kind")
        private final SourceKind sourceKind;

        @TableColumn(columnName = "youtube_channel_id")
        private final String youtubeChannelId;

        @TableConstructor
        public YoutubeChannel(ContentVideoPlatform contentVideoPlatform, SourceKind sourceKind, String youtubeChannelId) {
                super(contentVideoPlatform);
                if (sourceKind != SourceKind.YOUTUBE_CHANNEL) {
                        throw new IllegalArgumentException("SourceKind must be YOUTUBE_CHANNEL");
                }
                this.sourceKind = sourceKind;
                this.youtubeChannelId = youtubeChannelId;
        }

        public YoutubeChannel(ContentVideoPlatform contentVideoPlatform, String youtubeChannelId) {
                this(contentVideoPlatform, SourceKind.YOUTUBE_CHANNEL, youtubeChannelId);
        }

        public SourceKind getSourceKind() {
                return sourceKind;
        }

        public String getYoutubeChannelId() {
                return youtubeChannelId;
        }
}
