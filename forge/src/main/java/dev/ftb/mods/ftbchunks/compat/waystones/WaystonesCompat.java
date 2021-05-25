package dev.ftb.mods.ftbchunks.compat.waystones;

import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.KnownWaystonesEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

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

	public static Stream<IWaystone> getWaystones(ResourceKey<Level> dimension) {
		return WAYSTONES.stream().filter(w -> w.getDimension().equals(dimension));
	}

}
