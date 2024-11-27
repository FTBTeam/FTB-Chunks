package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftblibrary.config.NameMap;

public enum PointerIconMode {
    FACE(true, false),
    POINTER(false, true),
    BOTH(true, true),
    ;

    public static final NameMap<PointerIconMode> NAME_MAP = NameMap.of(BOTH, values()).baseNameKey("ftbchunks.minimap.pointer_icon_mode").create();

    private final boolean face;
    private final boolean pointer;

    PointerIconMode(boolean face, boolean pointer) {
        this.face = face;
        this.pointer = pointer;
    }

    public boolean showFace() {
        return face;
    }

    public boolean showPointer() {
        return pointer;
    }
}