package dev.ftb.mods.ftbchunks.integration.waystones;

import dev.ftb.mods.ftbchunks.integration.StaticMapIcon;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;

public class WaystoneMapIcon extends StaticMapIcon {
	public static final Icon ICON = Icon.getIcon("ftbchunks:textures/waystone.png");
	public static final Icon ICON_GLOBAL = ICON.withTint(Color4I.rgb(0xEB78E5));
	public final String name;
	public final boolean global;

	public WaystoneMapIcon(BlockPos pos, String n, boolean g) {
		super(pos);
		name = n;
		global = g;
		icon = global ? ICON_GLOBAL : ICON;
	}

	@Override
	public int getPriority() {
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
