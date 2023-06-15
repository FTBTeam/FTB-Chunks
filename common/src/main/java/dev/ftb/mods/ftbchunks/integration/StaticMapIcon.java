package dev.ftb.mods.ftbchunks.integration;

import dev.ftb.mods.ftbchunks.client.MapType;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class StaticMapIcon extends MapIcon {
	public final Vec3 pos;
	public Icon icon;

	public StaticMapIcon(Vec3 p) {
		pos = p;
		icon = Color4I.empty();
	}

	public StaticMapIcon(BlockPos p) {
		this(new Vec3(p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D));
	}

	@Override
	public Vec3 getPos(float delta) {
		return pos;
	}

	@Override
	public void draw(MapType mapType, GuiGraphics graphics, int x, int y, int w, int h, boolean outsideVisibleArea, int iconAlpha) {
		if (!icon.isEmpty()) {
			icon.draw(graphics, x, y, w, h);
		}
	}
}
