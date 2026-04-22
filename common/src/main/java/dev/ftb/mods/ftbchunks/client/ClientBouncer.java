package dev.ftb.mods.ftbchunks.client;

import dev.ftb.mods.ftbchunks.client.map.MapManager;

/// Don't reference any Minecraft clientside classes in here - it can be loaded server-side
public class ClientBouncer {
    public static void onConfigChanged(boolean fromServer) {
        if (!fromServer) {
            MapManager.getInstance().ifPresent(mgr -> mgr.updateAllRegions(false));
        }
    }
}
