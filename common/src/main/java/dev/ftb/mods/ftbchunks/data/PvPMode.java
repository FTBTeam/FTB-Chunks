package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftblibrary.util.NameMap;

public enum PvPMode {
    ALWAYS,
    NEVER,
    PER_TEAM,
    ;

    public static final NameMap<PvPMode> NAME_MAP = NameMap.of(ALWAYS, values()).baseNameKey("ftbchunks.pvp_mode").create();
}
