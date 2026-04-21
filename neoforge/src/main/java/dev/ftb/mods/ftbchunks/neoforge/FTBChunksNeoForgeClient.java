package dev.ftb.mods.ftbchunks.neoforge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.client.event.AddMapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.event.AddMinimapLayerEvent;
import dev.ftb.mods.ftbchunks.api.client.event.ChunksUpdatedFromServerEvent;
import dev.ftb.mods.ftbchunks.api.client.event.WaypointManagerAvailableEvent;
import dev.ftb.mods.ftbchunks.api.neoforge.FTBChunksClientEvent;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftblibrary.api.neoforge.FTBLibraryEvent;
import dev.ftb.mods.ftblibrary.platform.Platform;
import dev.ftb.mods.ftblibrary.util.neoforge.NeoEventHelper;
import dev.ftb.mods.ftbteams.api.neoforge.FTBTeamsEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.event.lifecycle.ClientStartedEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = FTBChunks.MOD_ID, dist = Dist.CLIENT)
public class FTBChunksNeoForgeClient {
    private final FTBChunksClient client;

    public FTBChunksNeoForgeClient() {
        client = FTBChunksClient.INSTANCE.init();

        IEventBus bus = NeoForge.EVENT_BUS;

        bus.addListener(ClientStartedEvent.class, _ -> client.onClientStarted(Minecraft.getInstance()));
        bus.addListener(ClientPlayerNetworkEvent.LoggingOut.class, _ -> client.onPlayerQuit());
        bus.addListener(ClientTickEvent.Pre.class, _ -> client.onClientTick(Minecraft.getInstance()));

        bus.addListener(ExtractLevelRenderStateEvent.class, event ->
                client.getInWorldIconRenderer().extractRenderState(event.getRenderState()));
        bus.addListener(RenderLevelStageEvent.AfterTranslucentParticles.class, _ ->
                client.getInWorldIconRenderer().renderBeacons());
        bus.addListener(RenderGuiEvent.Post.class, event -> client.onRenderHUD(event.getGuiGraphics(), event.getPartialTick()));
        bus.addListener(ScreenEvent.Init.Pre.class, event -> client.onGuiInit(event.getScreen()));

        bus.addListener(FTBLibraryEvent.CustomClick.class, event -> {
            if (client.handleCustomClick(event.getEventData().id())) {
                event.setCanceled(true);
            }
        });
        bus.addListener(FTBTeamsEvent.TeamPropertiesChanged.class, _ -> client.onTeamPropertiesChanged());
        bus.addListener(FTBChunksClientEvent.MapIcon.class, event -> client.onMapIconEvent(event.getEventData()));

        if (Platform.get().isDev()) {
            bus.addListener(FTBChunksClientEvent.WaypointManagerAvailable.class,
                    event -> client.onWaypointManagerAvailable(event.getEventData()));
            bus.addListener(FTBChunksClientEvent.AddMinimapLayer.class,
                    event -> client.addTestMinimapLayer(event.getEventData()));
        }

        registerNeoEventPosters(bus);
    }

    private static void registerNeoEventPosters(IEventBus bus) {
        NeoEventHelper.registerNeoEventPoster(bus, AddMapIconEvent.Data.class, FTBChunksClientEvent.MapIcon::new);
        NeoEventHelper.registerNeoEventPoster(bus, WaypointManagerAvailableEvent.Data.class, FTBChunksClientEvent.WaypointManagerAvailable::new);
        NeoEventHelper.registerNeoEventPoster(bus, AddMinimapLayerEvent.Data.class, FTBChunksClientEvent.AddMinimapLayer::new);
        NeoEventHelper.registerNeoEventPoster(bus, ChunksUpdatedFromServerEvent.Data.class, FTBChunksClientEvent.ChunksUpdatedFromServer::new);
    }
}
