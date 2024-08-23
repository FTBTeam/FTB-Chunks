package dev.ftb.mods.ftbchunks;

import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.config.BaseValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class StringBoolMapValue extends BaseValue<Map<String, Boolean>> {
    public StringBoolMapValue(@Nullable SNBTConfig c, String n, Map<String, Boolean> def) {
        super(c, n, def);
        super.set(new HashMap<>(def));
    }

    @Override
    public void write(SNBTCompoundTag tag) {
        Map<String, Boolean> map = get();
        SNBTCompoundTag mapTag = new SNBTCompoundTag();

        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            mapTag.putBoolean(entry.getKey(), entry.getValue());
        }

        tag.put(key, mapTag);
    }

    @Override
    public void read(SNBTCompoundTag tag) {
        Map<String, Boolean> map = new HashMap<>();

        for (String key : tag.getAllKeys()) {
            map.put(key, tag.getBoolean(key));
        }

        set(map);
    }
}
