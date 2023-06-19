package dev.ftb.mods.ftbchunks.integration;

import dev.ftb.mods.ftbchunks.client.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.MapType;
import dev.ftb.mods.ftbchunks.net.TeleportFromMapPacket;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public abstract class MapIcon {
	public abstract Vec3 getPos(float delta);

	public boolean isVisible(MapType mapType, double distanceToPlayer, boolean outsideVisibleArea) {
		return !mapType.isWorldIcon();
	}

	public double getIconScale(MapType mapType) {
		return 1D;
	}

	public boolean isIconOnEdge(MapType mapType, boolean outsideVisibleArea) {
		return false;
	}

	public boolean isZoomDependant(MapType mapType) {
		return false;
	}

	public int getPriority() {
		return 0;
	}

	public void addTooltip(TooltipList list) {
		list.styledString(String.format("%,d m", Mth.ceil(MathUtils.dist(Minecraft.getInstance().player.getX(), Minecraft.getInstance().player.getZ(), getPos(1F).x, getPos(1F).z))), ChatFormatting.GRAY);
	}

	public boolean mousePressed(LargeMapScreen screen, MouseButton button) {
		return false;
	}

	public boolean keyPressed(LargeMapScreen screen, Key key) {
		if (key.is(GLFW.GLFW_KEY_T)) {
			Vec3 v = getPos(1F);
			new TeleportFromMapPacket(Mth.floor(v.x), Mth.floor(v.y), Mth.floor(v.z), false, screen.dimension.dimension).sendToServer();
			screen.closeGui(false);
			return true;
		}

		return false;
	}

	public void draw(MapType mapType, GuiGraphics graphics, int x, int y, int w, int h, boolean outsideVisibleArea, int iconAlpha) {
	}
}
