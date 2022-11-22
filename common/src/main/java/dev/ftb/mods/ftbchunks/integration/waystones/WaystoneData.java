package dev.ftb.mods.ftbchunks.integration.waystones;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record WaystoneData(ResourceKey<Level> dimension, WaystoneMapIcon icon) {
}
