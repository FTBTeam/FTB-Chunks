package dev.ftb.mods.ftbchunks.util;

import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.config.BaseValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ChunkPosCustomYSetValue extends BaseValue<Set<ChunkPosWithMinY>> {
    private final HashMap<Long, Integer> lookup = new HashMap<>();

    public ChunkPosCustomYSetValue(@Nullable SNBTConfig c, String n, Set<ChunkPosWithMinY> def) {
        super(c, n, def);
        super.set(new HashSet<>());
    }

    @Override
    public void write(SNBTCompoundTag tag) {
        var listTag = new ListTag();

        for (ChunkPosWithMinY pos : get()) {
            var posTag = new SNBTCompoundTag();
            posTag.putInt("x", pos.chunkX());
            posTag.putInt("z", pos.chunkZ());
            posTag.putInt("min_y", pos.minY());
            listTag.add(posTag);
        }

        tag.put(key, listTag);
    }

    @Override
    public void read(SNBTCompoundTag tag) {
        var list = tag.getList(key, SNBTCompoundTag.class);
        Set<ChunkPosWithMinY> set = new HashSet<>();

        for (SNBTCompoundTag posTag : list) {
            int x = posTag.getIntOr("x", 0);
            int z = posTag.getIntOr("z", 0);
            int minY = posTag.getIntOr("min_y", 0);
            set.add(new ChunkPosWithMinY(x, z, minY));
        }

        set(set);
    }

    @Override
    public void set(Set<ChunkPosWithMinY> value) {
        super.set(value);

        lookup.clear();
        for (ChunkPosWithMinY pos : value) {
            lookup.put(ChunkPos.asLong(pos.chunkX(), pos.chunkZ()), pos.minY());
        }
    }

    public Map<Long, Integer> lookup() {
        return Collections.unmodifiableMap(lookup);
    }
}
