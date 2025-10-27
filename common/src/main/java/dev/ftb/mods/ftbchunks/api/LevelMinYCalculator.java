package dev.ftb.mods.ftbchunks.api;

import dev.ftb.mods.ftbchunks.api.event.CustomMinYEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.OptionalInt;

/**
 * Functional interface to allow custom minimum Y levels to be returned for a given dimension and block position.
 * The intention of this is to allow for custom areas of a map to be concealed from FTB Chunks mapping.
 * <p>
 * Listen to the Architectury {@link CustomMinYEvent} event to register this method, on both client and server.
 */
@FunctionalInterface
public interface LevelMinYCalculator {
    OptionalInt getLevelMinY(Level level, BlockPos pos);
}
