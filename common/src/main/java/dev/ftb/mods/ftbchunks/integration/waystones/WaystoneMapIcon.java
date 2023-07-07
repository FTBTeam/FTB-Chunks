package dev.ftb.mods.ftbchunks.integration.waystones;

import dev.ftb.mods.ftbchunks.client.mapicon.StaticMapIcon;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;

public class WaystoneMapIcon extends StaticMapIcon {
	private static final Icon ICON = Icon.getIcon("ftbchunks:textures/waystone.png");
	private static final Icon ICON_GLOBAL = ICON.withTint(Color4I.rgb(0xEB78E5));

	private final String name;
	private final boolean global;

	public WaystoneMapIcon(BlockPos pos, String name, boolean global) {
		super(pos);

		this.name = name;
		this.global = global;
		icon = this.global ? ICON_GLOBAL : ICON;
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
