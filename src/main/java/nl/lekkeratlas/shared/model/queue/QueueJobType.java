package nl.lekkeratlas.shared.model.queue;

import io.github.david.auk.fluid.jdbc.annotations.enums.EnumFormat;

@EnumFormat(db = EnumFormat.Strategy.lower_snake_case, local = EnumFormat.Strategy.UPPER_SNAKE_CASE)
public enum QueueJobType {
        FETCH_CREATOR_ACCOUNT_CONTENT,
        FETCH_CREATOR_ACCOUNT_METADATA,
        FETCH_VIDEO_METADATA,
}
