package nl.lekkeratlas.shared.model.content.contentplatform;

import io.github.david.auk.fluid.jdbc.annotations.enums.EnumFormat;

@EnumFormat(db = EnumFormat.Strategy.lower_snake_case, local = EnumFormat.Strategy.UPPER_SNAKE_CASE)
public enum SourceKind {
        YOUTUBE_CHANNEL
}
