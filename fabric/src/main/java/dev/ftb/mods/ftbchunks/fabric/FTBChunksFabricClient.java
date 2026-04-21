package dev.ftb.mods.ftbchunks.fabric;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.event.AddMapIconEvent;
import dev.ftb.mods.ftbchunks.api.client.event.AddMinimapLayerEvent;
import dev.ftb.mods.ftbchunks.api.client.event.ChunksUpdatedFromServerEvent;
import dev.ftb.mods.ftbchunks.api.client.event.WaypointManagerAvailableEvent;
import dev.ftb.mods.ftbchunks.api.fabric.FTBChunksClientEvents;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftblibrary.fabric.FTBLibraryFabricEvents;
import dev.ftb.mods.ftblibrary.platform.Platform;
import dev.ftb.mods.ftblibrary.util.fabric.FabricEventHelper;
import dev.ftb.mods.ftbteams.api.fabric.FTBTeamsEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public class FTBChunksFabricClient implements ClientModInitializer {
	private FTBChunksClient client;

	@Override
	public void onInitializeClient() {
		client = FTBChunksClient.INSTANCE.init();

		ClientLifecycleEvents.CLIENT_STARTED.register(client::onClientStarted);
		ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> client.onPlayerQuit());
		ClientTickEvents.START_CLIENT_TICK.register(client::onClientTick);

		LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(this::renderInWorldBeacons);
		LevelRenderEvents.END_EXTRACTION.register(this::extractRenderState);

		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, FTBChunksAPI.id("default"),
				(graphics, deltaTracker) -> client.onRenderHUD(graphics, deltaTracker));
		ScreenEvents.AFTER_INIT.register((_, screen, _, _) -> client.onGuiInit(screen));

		FTBLibraryFabricEvents.CUSTOM_CLICK.register(data -> client.handleCustomClick(data.id()));
		FTBTeamsEvents.TEAM_PROPERTIES_CHANGED.register(_ -> client.onTeamPropertiesChanged());
		FTBChunksClientEvents.ADD_MAP_ICON.register(client::onMapIconEvent);

		if (Platform.get().isDev()) {
			FTBChunksClientEvents.WAYPOINT_MANAGER_AVAILABLE.register(client::onWaypointManagerAvailable);
			FTBChunksClientEvents.ADD_MINIMAP_LAYER.register(client::addTestMinimapLayer);
		}

		registerFabricEventPosters();
	}

	private void registerFabricEventPosters() {
		FabricEventHelper.registerFabricEventPoster(AddMapIconEvent.Data.class, FTBChunksClientEvents.ADD_MAP_ICON);
		FabricEventHelper.registerFabricEventPoster(AddMinimapLayerEvent.Data.class, FTBChunksClientEvents.ADD_MINIMAP_LAYER);
		FabricEventHelper.registerFabricEventPoster(ChunksUpdatedFromServerEvent.Data.class, FTBChunksClientEvents.CHUNKS_UPDATED_FROM_SERVER);
		FabricEventHelper.registerFabricEventPoster(WaypointManagerAvailableEvent.Data.class, FTBChunksClientEvents.WAYPOINT_MANAGER_AVAILABLE);
    }

	private void renderInWorldBeacons(LevelRenderContext levelRenderContext) {
		client.getInWorldIconRenderer().renderBeacons();
	}

	private void extractRenderState(LevelExtractionContext context) {
		client.getInWorldIconRenderer().extractRenderState(context.levelState());
	}
}
