package dev.ftb.mods.ftbchunks.util;

import dev.ftb.mods.ftblibrary.config.value.AbstractListValue;
import dev.ftb.mods.ftblibrary.config.value.Config;
import net.minecraft.world.level.ChunkPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChunkPosCustomYSetValue extends AbstractListValue<ChunkPosWithMinY> {
    private final HashMap<Long, Integer> lookup = new HashMap<>();

    public ChunkPosCustomYSetValue(Config parent, String key, List<ChunkPosWithMinY> defaultValue) {
        super(parent, key, defaultValue, ChunkPosWithMinY.CODEC);
    }

    @Override
    public void set(List<ChunkPosWithMinY> value) {
        super.set(value);

        lookup.clear();
        for (ChunkPosWithMinY pos : value) {
            lookup.put(ChunkPos.pack(pos.chunkX(), pos.chunkZ()), pos.minY());
        }
    }

    public Map<Long, Integer> lookup() {
        return Collections.unmodifiableMap(lookup);
    }
}
