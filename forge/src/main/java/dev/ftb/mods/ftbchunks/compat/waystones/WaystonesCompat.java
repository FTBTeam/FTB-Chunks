package dev.ftb.mods.ftbchunks.compat.waystones;

import dev.ftb.mods.ftbchunks.integration.MapIconEvent;
import dev.ftb.mods.ftbchunks.integration.RefreshMinimapIconsEvent;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.KnownWaystonesEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaystonesCompat {
	private static final Map<ResourceKey<Level>, List<WaystoneMapIcon>> WAYSTONES = new HashMap<>();
	public static final Icon ICON = Icon.getIcon("ftbchunks:textures/waystone.png");
	public static final Icon ICON_GLOBAL = ICON.withTint(Color4I.rgb(0xEB78E5));

	public static void init() {
		MinecraftForge.EVENT_BUS.register(WaystonesCompat.class);
		MapIconEvent.MINIMAP.register(WaystonesCompat::mapWidgets);
		MapIconEvent.LARGE_MAP.register(WaystonesCompat::mapWidgets);
	}

	@SubscribeEvent
	public static void onWaystonesReceived(KnownWaystonesEvent event) {
		WAYSTONES.clear();

		for (IWaystone w : event.getWaystones()) {
			WAYSTONES.computeIfAbsent(w.getDimension(), k -> new ArrayList<>()).add(new WaystoneMapIcon(w));
		}

		RefreshMinimapIconsEvent.trigger();
	}

	private static void mapWidgets(MapIconEvent event) {
		List<WaystoneMapIcon> list = WAYSTONES.getOrDefault(event.getDimension(), Collections.emptyList());

		if (!list.isEmpty()) {
			for (WaystoneMapIcon icon : list) {
				event.add(icon);
			}
		}
	}
}
