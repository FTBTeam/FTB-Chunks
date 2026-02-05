package dev.ftb.mods.ftbchunks.client.minimap.components;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.FTBChunksClientAPI;

public class MinimapComponentSetup {
    public static void registerComponents() {
        FTBChunksClientAPI clientApi = FTBChunksAPI.clientApi();

        clientApi.registerMinimapComponent(new PlayerPosInfoComponent());
        clientApi.registerMinimapComponent(new ZoneInfoComponent());
        clientApi.registerMinimapComponent(new BiomeComponent());
        clientApi.registerMinimapComponent(new GameTimeComponent());
        clientApi.registerMinimapComponent(new RealTimeComponent());
        clientApi.registerMinimapComponent(new FPSComponent());
        clientApi.registerMinimapComponent(new DebugComponent());
    }
}
