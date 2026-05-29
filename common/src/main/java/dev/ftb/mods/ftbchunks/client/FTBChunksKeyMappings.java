package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.platform.client.PlatformClient;
import dev.ftb.mods.ftblibrary.platform.client.input.InputHelper;
import net.minecraft.client.KeyMapping;

public class FTBChunksKeyMappings {
    private static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(FTBChunksAPI.id("keys"));

    public static final KeyMapping MAP_KEY = InputHelper.createSimpleKeyMapping("map", KEY_CATEGORY, InputConstants.KEY_M);
    public static final KeyMapping TOGGLE_MINIMAP_KEY = InputHelper.createSimpleKeyMapping("toggle_minimap", KEY_CATEGORY);
    public static final KeyMapping ZOOM_IN_KEY = InputHelper.createSimpleKeyMapping("zoom_in", KEY_CATEGORY, InputConstants.KEY_EQUALS);
    public static final KeyMapping ZOOM_OUT_KEY = InputHelper.createSimpleKeyMapping("zoom_out", KEY_CATEGORY, InputConstants.KEY_MINUS);
    public static final KeyMapping CLAIM_MANAGER_KEY = InputHelper.createSimpleKeyMapping("claim_manager", KEY_CATEGORY);
    public static final KeyMapping ADD_WAYPOINT_KEY = InputHelper.createSimpleKeyMapping("add_waypoint", KEY_CATEGORY);
    public static final KeyMapping WAYPOINT_MANAGER_KEY = InputHelper.createSimpleKeyMapping("waypoint_manager", KEY_CATEGORY);

    static void init() {
        PlatformClient.get().input().registerKeyMapping(FTBChunksAPI.MOD_ID,
                MAP_KEY,
                TOGGLE_MINIMAP_KEY,
                ZOOM_IN_KEY,
                ZOOM_OUT_KEY,
                CLAIM_MANAGER_KEY,
                ADD_WAYPOINT_KEY,
                WAYPOINT_MANAGER_KEY
        );
    }
}
