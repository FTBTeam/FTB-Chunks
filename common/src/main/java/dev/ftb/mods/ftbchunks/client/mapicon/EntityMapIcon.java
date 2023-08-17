package dev.ftb.mods.ftbchunks.client.mapicon;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftbchunks.api.client.icon.MapIcon;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.integration.StaticMapIcon;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

public class EntityMapIcon implements MapIcon {
	private final Entity entity;
	private final Icon icon;

	public EntityMapIcon(Entity entity, Icon icon) {
		this.entity = entity;
		this.icon = icon;
	}

	@Override
	public Vec3 getPos(float partialTick) {
		return partialTick >= 1F ? entity.position() : entity.getPosition(partialTick);
	}

	@Override
	public boolean isVisible(MapType mapType, double distanceToPlayer, boolean outsideVisibleArea) {
		return !mapType.isWorldIcon() && (entity instanceof AbstractClientPlayer || !outsideVisibleArea);
	}

	@Override
	public double getIconScale(MapType mapType) {
		return entity instanceof AbstractClientPlayer || !mapType.isMinimap() || FTBChunksClientConfig.MINIMAP_LARGE_ENTITIES.get() ? 1D : (1D / 1.5D);
	}

	@Override
	public boolean isZoomDependant(MapType mapType) {
		return !(entity instanceof AbstractClientPlayer);
	}

	@Override
	public int getPriority() {
		return entity instanceof LocalPlayer ? 150 : entity instanceof AbstractClientPlayer ? 100 : 0;
	}

	@Override
	public void addTooltip(TooltipList list) {
		list.add(entity.getName());
	}

	@Override
	public boolean onMousePressed(BaseScreen screen, MouseButton button) {
		return false;
	}

	@Override
	public boolean onKeyPressed(BaseScreen screen, Key key) {
		return !(entity instanceof LocalPlayer) && StaticMapIcon.handleKeypress(this, screen, key);
	}

	@Override
	public void draw(MapType mapType, GuiGraphics graphics, int x, int y, int w, int h, boolean outsideVisibleArea, int iconAlpha) {
		if (icon instanceof ImageIcon) {
			var manager = Minecraft.getInstance().getTextureManager();
			var tex = manager.getTexture(((ImageIcon) icon).texture);

			if (tex == null) {
				tex = new SimpleTexture(((ImageIcon) icon).texture);
				manager.register(((ImageIcon) icon).texture, tex);
			}

			RenderSystem.bindTextureForSetup(tex.getId());
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, w > 4 ? GL11.GL_NEAREST : GL11.GL_LINEAR);
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, w > 4 ? GL11.GL_NEAREST : GL11.GL_LINEAR);
		}

		if (!(entity instanceof AbstractClientPlayer) || mapType.isMinimap() || w < 4 || icon == EntityIcons.NORMAL || icon == EntityIcons.HOSTILE) {
			icon.draw(graphics, x, y, w, h);
		} else {
			PoseStack poseStack = graphics.pose();
			poseStack.pushPose();
			poseStack.translate(x, y, 0F);
			poseStack.scale(w / 18F, h / 18F, 1F);
			Color4I.BLACK.draw(graphics, 0, 0, 18, 18);
			icon.draw(graphics, 1, 1, 16, 16);
			poseStack.popPose();
		}
	}
}
