package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.client.map.PlayerHeadTexture;
import com.feed_the_beast.mods.ftbguilibrary.icon.ImageIcon;
import com.feed_the_beast.mods.ftbguilibrary.utils.TooltipList;
import com.feed_the_beast.mods.ftbguilibrary.widget.Panel;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.feed_the_beast.mods.ftbguilibrary.widget.Widget;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.opengl.GL11;

/**
 * @author LatvianModder
 */
public class PlayerButton extends Widget
{
	public final ITextComponent name;
	public final String uuid;
	private final ResourceLocation texture;
	public final double playerX, playerZ;
	public final float rotation;

	public PlayerButton(Panel panel, AbstractClientPlayerEntity e)
	{
		super(panel);
		name = e.getDisplayName();
		uuid = UUIDTypeAdapter.fromUUID(e.getUUID());
		texture = new ResourceLocation("head", uuid);
		playerX = e.getX();
		playerZ = e.getZ();
		rotation = e.getYHeadRot();
	}

	@Override
	public void addMouseOverText(TooltipList list)
	{
		list.add(name);
	}

	@Override
	public void draw(MatrixStack matrixStack, Theme theme, int x, int y, int w, int h)
	{
		TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
		Texture t = texturemanager.getTexture(texture);
		if (t == null)
		{
			t = new PlayerHeadTexture("https://minotar.net/avatar/" + uuid + "/8", ImageIcon.MISSING_IMAGE);
			texturemanager.register(texture, t);
		}

		Matrix4f m = matrixStack.last().pose();

		RenderSystem.bindTexture(t.getId());
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
		buffer.vertex(m, x, y + h, 0).color(255, 255, 255, 255).uv(0F, 1F).endVertex();
		buffer.vertex(m, x + w, y + h, 0).color(255, 255, 255, 255).uv(1F, 1F).endVertex();
		buffer.vertex(m, x + w, y, 0).color(255, 255, 255, 255).uv(1F, 0F).endVertex();
		buffer.vertex(m, x, y, 0).color(255, 255, 255, 255).uv(0F, 0F).endVertex();
		tessellator.end();
	}
}