package dev.ftb.mods.ftbchunks.compat.waystones;

import dev.ftb.mods.ftbchunks.client.MinimapRenderer;
import dev.ftb.mods.ftbchunks.client.RegionMapPanel;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.KnownWaystonesEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collections;
import java.util.List;

public class WaystonesCompat {
	public static void init() {
		MinecraftForge.EVENT_BUS.register(WaystonesCompat.class);
	}

	private static List<IWaystone> WAYSTONES = Collections.emptyList();

	private static final int GLOBAL_COLOR = 0xeb78e5;

	public static int colorFor(IWaystone waystone) {
		return waystone.isGlobal() ? GLOBAL_COLOR : 0xFFFFFF;
	}

	@SubscribeEvent
	public static void onWaystonesReceived(KnownWaystonesEvent event) {
		WAYSTONES = event.getWaystones();
	}

	public static void addWidgets(RegionMapPanel panel) {
		for (IWaystone waystone : WAYSTONES) {
			if (waystone.getDimension() == panel.largeMap.dimension.dimension) {
				panel.add(new WaystoneWidget(panel, waystone));
			}
		}
	}

	public static void renderMinimap(MapDimension dimension, MinimapRenderer renderer) {
		for (IWaystone waystone : WAYSTONES) {
			if (waystone.getDimension() == dimension.dimension) {
				renderer.render(waystone.getPos().getX(), waystone.getPos().getZ(), colorFor(waystone), 0);
			}
		}
	}
}
