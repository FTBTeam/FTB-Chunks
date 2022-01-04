package dev.ftb.mods.ftbchunks.compat.waystones;

import dev.ftb.mods.ftbchunks.integration.StaticMapIcon;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.blay09.mods.waystones.api.IWaystone;
import net.minecraft.ChatFormatting;

public class WaystoneMapIcon extends StaticMapIcon {
	public final String name;
	public final boolean global;

	public WaystoneMapIcon(IWaystone w) {
		super(w.getPos());
		name = w.getName();
		global = w.isGlobal();
		icon = global ? WaystonesCompat.ICON_GLOBAL : WaystonesCompat.ICON;
	}

	@Override
	public int getImportance() {
		return 50;
	}

	@Override
	public void addTooltip(TooltipList list) {
		list.string(name);

		if (global) {
			list.styledString("Global", ChatFormatting.LIGHT_PURPLE);
		}

		super.addTooltip(list);
	}
}
