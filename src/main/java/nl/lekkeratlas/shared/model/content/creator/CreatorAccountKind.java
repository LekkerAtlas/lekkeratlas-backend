package nl.lekkeratlas.shared.model.content.creator;

import io.github.david.auk.fluid.jdbc.annotations.enums.EnumFormat;

@EnumFormat(db = EnumFormat.Strategy.lower_snake_case, local = EnumFormat.Strategy.UPPER_SNAKE_CASE)
public enum CreatorAccountKind {
        YOUTUBE_CHANNEL
}
