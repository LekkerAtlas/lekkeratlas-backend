package nl.lekkeratlas.shared.model.queue;

import io.github.david.auk.fluid.jdbc.annotations.enums.EnumFormat;

@EnumFormat(db = EnumFormat.Strategy.lower_snake_case, local = EnumFormat.Strategy.UPPER_SNAKE_CASE)
public enum QueueJobType {
        FETCH_PLATFORM_CONTENT,
        FETCH_CHANNEL_METADATA,
        FETCH_VIDEO_METADATA,
}
