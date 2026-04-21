package dev.ftb.mods.ftbchunks.util.platform;

import dev.ftb.mods.ftbchunks.api.Protection;

public interface PlatformProtections {
    Protection blockBreakProtection();

    Protection blockInteractProtection();

    Protection blockPlaceProtection();

    record Impl(
            Protection blockBreakProtection,
            Protection blockInteractProtection,
            Protection blockPlaceProtection
    ) implements PlatformProtections { }
}
