package dev.ftb.mods.ftbchunks.client.config;

import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.config.BaseValue;
import dev.ftb.mods.ftblibrary.snbt.config.SNBTConfig;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class StringMapValue extends BaseValue<Map<String, String>> {

    public StringMapValue(@Nullable SNBTConfig c, String n, Map<String, String> def) {
        super(c, n, def);
        super.set(new HashMap<>(def));
    }

    @Override
    public void write(SNBTCompoundTag tag) {
        Map<String, String> map = get();
        SNBTCompoundTag mapTag = new SNBTCompoundTag();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            mapTag.putString(entry.getKey(), entry.getValue());
        }

        tag.put(key, mapTag);
    }

    @Override
    public void read(SNBTCompoundTag tag) {
        Map<String, String> map = new HashMap<>();

        for (String key : tag.getAllKeys()) {
            map.put(key, tag.getString(key));
        }

        set(map);
    }
}
