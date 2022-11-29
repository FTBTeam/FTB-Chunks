package dev.ftb.mods.ftbchunks.data;

import dev.ftb.mods.ftblibrary.config.NameMap;

public enum PartyLimitMode {
    LARGEST,
    OWNER,
    SUM,
    AVERAGE;

    public static final NameMap<PartyLimitMode> NAME_MAP = NameMap.of(LARGEST, values()).create();
}
