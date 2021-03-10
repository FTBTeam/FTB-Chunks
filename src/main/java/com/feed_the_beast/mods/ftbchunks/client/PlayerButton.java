package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.client.map.PlayerHeadTexture;
import com.feed_the_beast.mods.ftbguilibrary.icon.ImageIcon;
import com.feed_the_beast.mods.ftbguilibrary.utils.TooltipList;
import com.feed_the_beast.mods.ftbguilibrary.widget.Panel;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.feed_the_beast.mods.ftbguilibrary.widget.Widget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * @author LatvianModder
 */
public class PlayerButton extends Widget {
	public final Component name;
	public final String uuid;
	private final ResourceLocation texture;
	public final double playerX, playerZ;
	public final float rotation;

	public PlayerButton(Panel panel, AbstractClientPlayer e) {
		super(panel);
		name = e.getDisplayName();
		uuid = UUIDTypeAdapter.fromUUID(e.getUUID());
		texture = new ResourceLocation("head", uuid);
		playerX = e.getX();
		playerZ = e.getZ();
		rotation = e.getYHeadRot();
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		list.add(name);
	}

	@Override
	public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
		AbstractTexture t = texturemanager.getTexture(texture);
		if (t == null) {
			t = new PlayerHeadTexture("https://minotar.net/avatar/" + uuid + "/8", ImageIcon.MISSING_IMAGE);
			texturemanager.register(texture, t);
		}

		Matrix4f m = matrixStack.last().pose();

		RenderSystem.bindTexture(t.getId());
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		buffer.vertex(m, x, y + h, 0).color(255, 255, 255, 255).uv(0F, 1F).endVertex();
		buffer.vertex(m, x + w, y + h, 0).color(255, 255, 255, 255).uv(1F, 1F).endVertex();
		buffer.vertex(m, x + w, y, 0).color(255, 255, 255, 255).uv(1F, 0F).endVertex();
		buffer.vertex(m, x, y, 0).color(255, 255, 255, 255).uv(0F, 0F).endVertex();
		tessellator.end();
	}
}