package dev.ftb.mods.ftbchunks.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbchunks.client.MapType;
import dev.ftb.mods.ftblibrary.icon.Icon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class StaticMapIcon extends MapIcon {
	public final Vec3 pos;
	public Icon icon;

	public StaticMapIcon(Vec3 p) {
		pos = p;
		icon = Icon.EMPTY;
	}

	public StaticMapIcon(BlockPos p) {
		this(new Vec3(p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D));
	}

	@Override
	public Vec3 getPos(float delta) {
		return pos;
	}

	@Override
	public void draw(MapType mapType, PoseStack stack, int x, int y, int w, int h, boolean outsideVisibleArea) {
		if (icon != Icon.EMPTY) {
			icon.draw(stack, x, y, w, h);
		}
	}
}
