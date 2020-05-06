package com.feed_the_beast.mods.ftbchunks.client;

import com.feed_the_beast.mods.ftbchunks.client.map.ClientMapDimension;
import com.feed_the_beast.mods.ftbchunks.impl.map.XZ;
import com.feed_the_beast.mods.ftbchunks.net.FTBChunksNet;
import com.feed_the_beast.mods.ftbchunks.net.RequestPlayerListPacket;
import com.feed_the_beast.mods.ftbguilibrary.icon.Color4I;
import com.feed_the_beast.mods.ftbguilibrary.utils.ClientUtils;
import com.feed_the_beast.mods.ftbguilibrary.utils.Key;
import com.feed_the_beast.mods.ftbguilibrary.utils.MouseButton;
import com.feed_the_beast.mods.ftbguilibrary.widget.Button;
import com.feed_the_beast.mods.ftbguilibrary.widget.ColorWidget;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiBase;
import com.feed_the_beast.mods.ftbguilibrary.widget.GuiIcons;
import com.feed_the_beast.mods.ftbguilibrary.widget.SimpleButton;
import com.feed_the_beast.mods.ftbguilibrary.widget.Theme;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class LargeMapScreen extends GuiBase
{
	public Color4I backgroundColor = Color4I.rgb(0x202225);

	public RegionMapPanel regionPanel;
	public int zoom = 256;
	public final HashMap<XZ, RegionTextureData> regionTextures;
	public ClientMapDimension dimension;
	public int scrollWidth = 0;
	public int scrollHeight = 0;
	public int prevMouseX, prevMouseY;
	public int grabbed = 0;
	public boolean movedToPlayer = false;
	public Button claimChunksButton, dimensionButton, waypointsButton, settingsButton, alliesButton;

	public LargeMapScreen()
	{
		regionPanel = new RegionMapPanel(this);
		regionTextures = new HashMap<>();
		dimension = ClientMapDimension.current;
		regionPanel.setScrollX(0D);
		regionPanel.setScrollY(0D);
	}

	public int getRegionButtonSize()
	{
		return zoom * 2;
	}

	public void addZoom(double up)
	{
		int z = zoom;

		if (up > 0D)
		{
			zoom *= 2;
		}
		else
		{
			zoom /= 2;
		}

		zoom = MathHelper.clamp(zoom, 1, 1024);

		if (zoom != z)
		{
			grabbed = 0;
			double sx = regionPanel.regionX;
			double sy = regionPanel.regionZ;
			regionPanel.resetScroll();
			regionPanel.scrollTo(sx, sy);

			ObfuscationReflectionHelper.setPrivateValue(MouseHelper.class, Minecraft.getInstance().mouseHelper, true, "field_198051_p");
			Minecraft.getInstance().mouseHelper.ungrabMouse();
		}
	}

	@Override
	public void onClosed()
	{
		super.onClosed();

		for (RegionTextureData data : regionTextures.values())
		{
			data.release();
		}

		regionTextures.clear();
	}

	@Override
	@SuppressWarnings("deprecation")
	public void addWidgets()
	{
		add(regionPanel);

		add(new ColorWidget(this, backgroundColor.withAlpha(150), null).setPosAndSize(0, 0, 18, 55 - 18)); // TODO: Re-add waypoints button
		add(new ColorWidget(this, backgroundColor.withAlpha(150), null).setPosAndSize(0, height - 38, 18, 38));

		add(claimChunksButton = new SimpleButton(this, I18n.format("ftbchunks.gui.claimed_chunks"), GuiIcons.MAP, (b, m) -> new ChunkScreen().openGui()));
		/*
		add(waypointsButton = new SimpleButton(this, I18n.format("ftbchunks.gui.waypoints"), GuiIcons.BEACON, (b, m) -> {
			Minecraft.getInstance().getToastGui().add(new SystemToast(SystemToast.Type.TUTORIAL_HINT, new StringTextComponent("WIP!"), null));
		}));
		 */
		add(settingsButton = new SimpleButton(this, I18n.format("ftbchunks.gui.settings"), GuiIcons.SETTINGS, (b, m) -> FTBChunksClientConfig.openSettings()));
		add(alliesButton = new SimpleButton(this, I18n.format("ftbchunks.gui.allies"), GuiIcons.FRIENDS, (b, m) -> FTBChunksNet.MAIN.sendToServer(new RequestPlayerListPacket())));

		add(dimensionButton = new SimpleButton(this, dimension.directory.getFileName().toString(), GuiIcons.GLOBE, (b, m) -> {
			List<DimensionType> types = Registry.DIMENSION_TYPE.stream().filter(t -> {
				ResourceLocation id = DimensionType.getKey(t);
				return id != null && Files.exists(dimension.manager.directory.resolve(id.getNamespace() + "_" + id.getPath()));
			}).collect(Collectors.toList());

			int i = types.indexOf(dimension.dimension);

			if (i != -1)
			{
				for (RegionTextureData data : regionTextures.values())
				{
					data.release();
				}

				regionTextures.clear();

				dimension = dimension.manager.getDimension(types.get((i + 1) % types.size()));
				refreshWidgets();
				movedToPlayer = false;
			}
		}));
	}

	@Override
	public void alignWidgets()
	{
		claimChunksButton.setPosAndSize(1, 1, 16, 16);
		alliesButton.setPosAndSize(1, 19, 16, 16);
		//waypointsButton.setPosAndSize(1, 37, 16, 16);
		settingsButton.setPosAndSize(1, height - 18, 16, 16);
		dimensionButton.setPosAndSize(1, height - 36, 16, 16);
	}

	@Override
	public boolean onInit()
	{
		return setFullscreen();
	}

	@Override
	public boolean mousePressed(MouseButton button)
	{
		if (super.mousePressed(button))
		{
			return true;
		}

		if (button.isLeft())
		{
			prevMouseX = getMouseX();
			prevMouseY = getMouseY();
			return true;
		}

		return false;
	}

	@Override
	public boolean keyPressed(Key key)
	{
		if (key.is(GLFW.GLFW_KEY_T))
		{
			if (dimension == ClientMapDimension.current)
			{
				ClientUtils.execClientCommand("/teleport " + regionPanel.blockX + " ~2 " + regionPanel.blockZ, false);
				closeGui(false);
			}

			return true;
		}
		else if (key.is(GLFW.GLFW_KEY_W))
		{
			return true;
		}
		else if (key.is(GLFW.GLFW_KEY_SPACE))
		{
			movedToPlayer = false;
			return true;
		}

		return super.keyPressed(key);
	}

	@Override
	public boolean drawDefaultBackground()
	{
		if (!movedToPlayer)
		{
			PlayerEntity p = Minecraft.getInstance().player;
			regionPanel.resetScroll();
			regionPanel.scrollTo(p.chunkCoordX / 32D, p.chunkCoordZ / 32D);
			movedToPlayer = true;
		}

		backgroundColor.draw(0, 0, width, height);
		return false;
	}

	@Override
	public void drawBackground(Theme theme, int x, int y, int w, int h)
	{
		if (grabbed != 0)
		{
			int mx = getMouseX();
			int my = getMouseY();

			if (scrollWidth > regionPanel.width)
			{
				regionPanel.setScrollX(Math.max(Math.min(regionPanel.getScrollX() + (prevMouseX - mx), scrollWidth - regionPanel.width), 0));
			}

			if (scrollHeight > regionPanel.height)
			{
				regionPanel.setScrollY(Math.max(Math.min(regionPanel.getScrollY() + (prevMouseY - my), scrollHeight - regionPanel.height), 0));
			}

			prevMouseX = mx;
			prevMouseY = my;
		}

		if (scrollWidth <= regionPanel.width)
		{
			regionPanel.setScrollX((scrollWidth - regionPanel.width) / 2D);
		}

		if (scrollHeight <= regionPanel.height)
		{
			regionPanel.setScrollY((scrollHeight - regionPanel.height) / 2D);
		}

		GlStateManager.disableTexture();
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		int r = 70;
		int g = 70;
		int b = 70;
		int a = 100;

		buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

		int s = getRegionButtonSize();
		double ox = -regionPanel.getScrollX() % s;
		double oy = -regionPanel.getScrollY() % s;

		for (int gx = 0; gx <= (w / s) + 1; gx++)
		{
			buffer.pos(x + ox + gx * s, y, 0).color(r, g, b, a).endVertex();
			buffer.pos(x + ox + gx * s, y + h, 0).color(r, g, b, a).endVertex();
		}

		for (int gy = 0; gy <= (h / s) + 1; gy++)
		{
			buffer.pos(x, y + oy + gy * s, 0).color(r, g, b, a).endVertex();
			buffer.pos(x + w, y + oy + gy * s, 0).color(r, g, b, a).endVertex();
		}

		tessellator.draw();
		GlStateManager.enableTexture();
	}

	@Override
	public void drawForeground(Theme theme, int x, int y, int w, int h)
	{
		String coords = "X: " + regionPanel.blockX + ", Z: " + regionPanel.blockZ;
		int coordsw = theme.getStringWidth(coords) / 2;

		backgroundColor.withAlpha(150).draw(x + (w - coordsw) / 2, y + h - 6, coordsw + 4, h);
		RenderSystem.pushMatrix();
		RenderSystem.translatef(x + (w - coordsw) / 2F + 2F, y + h - 5, 0F);
		RenderSystem.scalef(0.5F, 0.5F, 1F);
		theme.drawString(coords, 0, 0, Theme.SHADOW);
		RenderSystem.popMatrix();
	}
}