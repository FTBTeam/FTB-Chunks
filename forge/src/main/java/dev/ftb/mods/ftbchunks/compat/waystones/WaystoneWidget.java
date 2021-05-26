package dev.ftb.mods.ftbchunks.compat.waystones;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbchunks.client.CustomMapWidget;
import dev.ftb.mods.ftbchunks.client.map.WaypointType;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.blay09.mods.waystones.api.IWaystone;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class WaystoneWidget extends CustomMapWidget {
	public final IWaystone waystone;
	public Icon icon;

	public WaystoneWidget(Panel panel, IWaystone waystone) {
		super(panel);
		this.icon = Icon.getIcon(WaypointType.WAYSTONE.texture).withTint(Color4I.rgb(WaystonesCompat.colorFor(waystone)));
		this.waystone = waystone;
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		list.string(waystone.getName());

		if (waystone.isGlobal()) {
			list.styledString("Global", ChatFormatting.LIGHT_PURPLE);
		}

		long dist = (long) MathUtils.dist(Minecraft.getInstance().player.getX(), Minecraft.getInstance().player.getZ(), waystone.getPos().getX(), waystone.getPos().getZ());
		list.styledString(dist + " m", ChatFormatting.GRAY);
	}

	@Override
	public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		icon.draw(matrixStack, x, y, w, h);
	}

	@Override
	public Vec3 getPos() {
		BlockPos pos = waystone.getPos();
		return new Vec3(pos.getX(), pos.getY(), pos.getZ());
	}
}
