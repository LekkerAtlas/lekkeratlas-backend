package nl.lekkeratlas.shared.model.content;

import io.github.david.auk.fluid.jdbc.annotations.enums.EnumFormat;

@EnumFormat(db = EnumFormat.Strategy.lower_snake_case, local = EnumFormat.Strategy.UPPER_SNAKE_CASE)
public enum ContentType {
    LIVE_STREAM,
    LIVE_STREAM_CLIP,
    OFFICIAL_VIDEO,
    FAN_MADE_VIDEO,
    LEKKER_SPELEN_RELATED,
    OTHER
}
