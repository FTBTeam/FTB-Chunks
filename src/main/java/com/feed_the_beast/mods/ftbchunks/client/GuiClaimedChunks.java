package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiBase;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

/**
 * @author LatvianModder
 */
public class GuiClaimedChunks extends GuiBase
{
	public final MinimapDimension dim;
	public int viewX, viewZ;

	public GuiClaimedChunks(MinimapData d)
	{
		PlayerEntity p = Minecraft.getInstance().player;
		dim = d.getDimension(p.world.getDimension().getType());
		BlockPos pos = p.getPosition();
		viewX = pos.getX();
		viewZ = pos.getZ();
	}

	@Override
	public boolean onInit()
	{
		return setFullscreen();
	}

	@Override
	public void addWidgets()
	{
		System.out.println(viewX + "x" + viewZ);
		System.out.println(width + "x" + height);
	}

	@Override
	public void drawBackground(Theme theme, int x, int y, int w, int h)
	{
		Color4I.BLACK.draw(x, y, w, h);

		int r = 70;
		int g = 70;
		int b = 70;
		int a = 100;

		GlStateManager.disableTexture();
		GlStateManager.lineWidth(1F);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

		for (int gy = 0; gy <= h / 16; gy++)
		{
			buffer.pos(x, y + gy * 16D, 0D).color(r, g, b, a).endVertex();
			buffer.pos(x + w, y + gy * 16D, 0D).color(r, g, b, a).endVertex();
		}

		for (int gx = 0; gx <= w / 16; gx++)
		{
			buffer.pos(x + gx * 16D, y, 0D).color(r, g, b, a).endVertex();
			buffer.pos(x + gx * 16D, y + h, 0D).color(r, g, b, a).endVertex();
		}

		tessellator.draw();
		GlStateManager.enableTexture();
	}
}