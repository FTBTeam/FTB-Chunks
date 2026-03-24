package dev.ftb.mods.ftbchunks.api;

import dev.ftb.mods.ftbchunks.api.event.CustomMinYEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.OptionalInt;

/// Functional interface to allow custom minimum Y levels to be returned for a given dimension and block position.
/// The intention of this is to allow for custom areas of a map to be concealed from FTB Chunks mapping.
///
/// Listen to the Architectury [CustomMinYEvent] event to register this method, on both client and server.
@FunctionalInterface
public interface LevelMinYCalculator {
    /// Return the minimum effective mapping level for a given level and block position.
    ///
    /// @param level the level
    /// @param pos the block position
    /// @return the minimum Y level, below which no mapping will be done, or `OptionalInt.empty()` if this method
    /// doesn't care about the given level and position
    OptionalInt getLevelMinY(Level level, BlockPos pos);
}
