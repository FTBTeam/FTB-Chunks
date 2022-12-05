package dev.ftb.mods.ftbchunks.compat.waystones;

import dev.ftb.mods.ftbchunks.integration.waystones.WaystoneData;
import dev.ftb.mods.ftbchunks.integration.waystones.WaystoneMapIcon;
import dev.ftb.mods.ftbchunks.integration.waystones.WaystonesCommon;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.KnownWaystonesEvent;

public class WaystonesCompat {
	public static void init() {
		Balm.getEvents().onEvent(KnownWaystonesEvent.class, WaystonesCompat::onKnownWaystones);
	}

	public static void onKnownWaystones(KnownWaystonesEvent event) {
		WaystonesCommon.updateWaystones(event.getWaystones().stream().map(w -> new WaystoneData(w.getDimension(), new WaystoneMapIcon(w.getPos(), w.getName(), w.isGlobal()))).toList());
	}
}
