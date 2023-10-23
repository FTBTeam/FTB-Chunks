package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftblibrary.config.NameMap;

public enum PvPMode {
    ALWAYS,
    NEVER,
    PER_TEAM,
    ;

    public static final NameMap<PvPMode> NAME_MAP = NameMap.of(ALWAYS, values()).create();
}
