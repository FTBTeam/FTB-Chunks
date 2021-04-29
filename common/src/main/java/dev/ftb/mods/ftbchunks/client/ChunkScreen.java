package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.util.UUIDTypeAdapter;
import dev.ftb.mods.ftbchunks.FTBCUtils;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.PlayerHeadTexture;
import dev.ftb.mods.ftbchunks.net.FTBChunksNet;
import dev.ftb.mods.ftbchunks.net.RequestChunkChangePacket;
import dev.ftb.mods.ftbchunks.net.RequestMapDataPacket;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.SimpleButton;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author LatvianModder
 */
public class ChunkScreen extends BaseScreen {
	public class ChunkButton extends Button {
		public final XZ chunkPos;
		public MapChunk chunk;

		public ChunkButton(Panel panel, XZ xz) {
			super(panel, TextComponent.EMPTY, Icon.EMPTY);
			setSize(FTBChunks.TILE_SIZE, FTBChunks.TILE_SIZE);
			chunkPos = xz;
		}

		@Override
		public void onClicked(MouseButton mouseButton) {
			selectedChunks.add(chunkPos);
		}

		@Override
		public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
			if (isMouseOver() || selectedChunks.contains(chunkPos)) {
				Color4I.WHITE.withAlpha(100).draw(matrixStack, x, y, w, h);

				if (isMouseButtonDown(MouseButton.LEFT) || isMouseButtonDown(MouseButton.RIGHT)) {
					selectedChunks.add(chunkPos);
				}
			}
		}

		@Override
		@SuppressWarnings("deprecation")
		public void addMouseOverText(TooltipList list) {
			if (chunk != null && chunk.team != null) {
				list.add(chunk.team.getName());

				Date date = new Date();

				if (Screen.hasAltDown()) {
					list.add(new TextComponent(chunk.claimedDate.toLocaleString()).withStyle(ChatFormatting.GRAY));
				} else {
					list.add(new TextComponent(FTBCUtils.prettyTimeString((date.getTime() - chunk.claimedDate.getTime()) / 1000L) + " ago").withStyle(ChatFormatting.GRAY));
				}

				if (chunk.forceLoadedDate != null) {
					list.add(new TranslatableComponent("ftbchunks.gui.force_loaded").withStyle(ChatFormatting.RED));

					if (Screen.hasAltDown()) {
						list.add(new TextComponent(chunk.forceLoadedDate.toLocaleString()).withStyle(ChatFormatting.GRAY));
					} else {
						list.add(new TextComponent(FTBCUtils.prettyTimeString((date.getTime() - chunk.forceLoadedDate.getTime()) / 1000L) + " ago").withStyle(ChatFormatting.GRAY));
					}
				}
			}
		}
	}

	public MapDimension dimension = MapDimension.getCurrent();
	public List<ChunkButton> chunkButtons;
	public Set<XZ> selectedChunks;

	public ChunkScreen() {
		FTBChunksClient.alwaysRenderChunksOnMap = true;

		if (MapManager.inst != null) {
			MapManager.inst.updateAllRegions(false);
		}
	}

	@Override
	public boolean onInit() {
		return setFullscreen();
	}

	@Override
	public void onClosed() {
		FTBChunksClient.alwaysRenderChunksOnMap = false;

		if (MapManager.inst != null) {
			MapManager.inst.updateAllRegions(false);
		}

		super.onClosed();
	}

	@Override
	public void addWidgets() {
		int sx = getX() + (width - FTBChunks.MINIMAP_SIZE) / 2;
		int sy = getY() + (height - FTBChunks.MINIMAP_SIZE) / 2;
		Player player = Minecraft.getInstance().player;
		int startX = player.xChunk - FTBChunks.TILE_OFFSET;
		int startZ = player.zChunk - FTBChunks.TILE_OFFSET;

		chunkButtons = new ArrayList<>();
		selectedChunks = new LinkedHashSet<>();

		for (int z = 0; z < FTBChunks.TILES; z++) {
			for (int x = 0; x < FTBChunks.TILES; x++) {
				ChunkButton button = new ChunkButton(this, XZ.of(startX + x, startZ + z));
				button.chunk = dimension.getRegion(XZ.regionFromChunk(startX + x, startZ + z)).getDataBlocking().getChunk(button.chunkPos);
				chunkButtons.add(button);
				button.setPos(sx + x * FTBChunks.TILE_SIZE, sy + z * FTBChunks.TILE_SIZE);
			}
		}

		addAll(chunkButtons);
		FTBChunksNet.MAIN.sendToServer(new RequestMapDataPacket(player.xChunk - FTBChunks.TILE_OFFSET, player.zChunk - FTBChunks.TILE_OFFSET, player.xChunk + FTBChunks.TILE_OFFSET, player.zChunk + FTBChunks.TILE_OFFSET));
		add(new SimpleButton(this, new TranslatableComponent("ftbchunks.gui.large_map"), Icons.MAP, (simpleButton, mouseButton) -> new LargeMapScreen().openGui()).setPosAndSize(1, 1, 16, 16));
		// add(new SimpleButton(this, new TranslatableComponent("ftbchunks.gui.allies"), Icons.FRIENDS, (simpleButton, mouseButton) -> {}).setPosAndSize(1, 19, 16, 16));
	}

	@Override
	public void mouseReleased(MouseButton button) {
		super.mouseReleased(button);

		if (!selectedChunks.isEmpty()) {
			FTBChunksNet.MAIN.sendToServer(new RequestChunkChangePacket(isShiftKeyDown() ? (button.isLeft() ? 2 : 3) : (button.isLeft() ? 0 : 1), selectedChunks));
			selectedChunks.clear();
			playClickSound();
		}
	}

	@Override
	public boolean keyPressed(Key key) {
		if (key.is(GLFW.GLFW_KEY_F)) {
			new LargeMapScreen().openGui();
			return true;
		}

		return super.keyPressed(key);
	}

	@Override
	public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		Player player = Minecraft.getInstance().player;
		int startX = player.xChunk - FTBChunks.TILE_OFFSET;
		int startZ = player.zChunk - FTBChunks.TILE_OFFSET;

		int sx = x + (w - FTBChunks.MINIMAP_SIZE) / 2;
		int sy = y + (h - FTBChunks.MINIMAP_SIZE) / 2;

		int r = 70;
		int g = 70;
		int b = 70;
		int a = 100;

		RenderSystem.lineWidth(Math.max(2.5F, (float) Minecraft.getInstance().getWindow().getWidth() / 1920.0F * 2.5F));

		RenderSystem.enableTexture();
		RenderSystem.bindTexture(FTBChunksClient.minimapTextureId);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GuiHelper.drawTexturedRect(matrixStack, sx, sy, FTBChunks.MINIMAP_SIZE, FTBChunks.MINIMAP_SIZE, Color4I.WHITE, 0F, 0F, 1F, 1F);

		if (!InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_TAB)) {
			RenderSystem.disableTexture();

			buffer.begin(GL11.GL_LINES, DefaultVertexFormat.POSITION_COLOR);

			for (int gy = 1; gy < FTBChunks.TILES; gy++) {
				buffer.vertex(sx, sy + gy * FTBChunks.TILE_SIZE, 0).color(r, g, b, a).endVertex();
				buffer.vertex(sx + FTBChunks.MINIMAP_SIZE, sy + gy * FTBChunks.TILE_SIZE, 0).color(r, g, b, a).endVertex();
			}

			for (int gx = 1; gx < FTBChunks.TILES; gx++) {
				buffer.vertex(sx + gx * FTBChunks.TILE_SIZE, sy, 0).color(r, g, b, a).endVertex();
				buffer.vertex(sx + gx * FTBChunks.TILE_SIZE, sy + FTBChunks.MINIMAP_SIZE, 0).color(r, g, b, a).endVertex();
			}

			tessellator.end();

			buffer.begin(GL11.GL_LINES, DefaultVertexFormat.POSITION_COLOR);

			for (ChunkButton button : chunkButtons) {
				MapChunk chunk = button.chunk;

				if (chunk.forceLoadedDate == null) {
					continue;
				}

				int cx = button.getX();
				int cy = button.getY();

				buffer.vertex(cx, cy, 0).color(255, 0, 0, 100).endVertex();
				buffer.vertex(cx + FTBChunks.TILE_SIZE, cy + FTBChunks.TILE_SIZE, 0).color(255, 0, 0, 100).endVertex();

				buffer.vertex(cx + FTBChunks.TILE_SIZE / 2F, cy, 0).color(255, 0, 0, 100).endVertex();
				buffer.vertex(cx + FTBChunks.TILE_SIZE, cy + FTBChunks.TILE_SIZE / 2F, 0).color(255, 0, 0, 100).endVertex();

				buffer.vertex(cx, cy + FTBChunks.TILE_SIZE / 2F, 0).color(255, 0, 0, 100).endVertex();
				buffer.vertex(cx + FTBChunks.TILE_SIZE / 2F, cy + FTBChunks.TILE_SIZE, 0).color(255, 0, 0, 100).endVertex();
			}

			tessellator.end();
		}

		RenderSystem.enableTexture();
		RenderSystem.lineWidth(1F);

		String uuid = UUIDTypeAdapter.fromUUID(player.getUUID());
		ResourceLocation headTextureLocation = new ResourceLocation("uuid", uuid);
		AbstractTexture headTexture = texturemanager.getTexture(headTextureLocation);
		if (headTexture == null) {
			headTexture = new PlayerHeadTexture("https://minotar.net/avatar/" + uuid + "/8", ImageIcon.MISSING_IMAGE);
			texturemanager.register(headTextureLocation, headTexture);
		}

		double hx = sx + FTBChunks.TILE_SIZE * FTBChunks.TILE_OFFSET + MathUtils.mod(player.getX(), 16D);
		double hy = sy + FTBChunks.TILE_SIZE * FTBChunks.TILE_OFFSET + MathUtils.mod(player.getZ(), 16D);

		RenderSystem.bindTexture(headTexture.getId());
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		buffer.vertex(hx - 4, hy + 4, 0.0D).color(255, 255, 255, 255).uv(0F, 1F).endVertex();
		buffer.vertex(hx + 4, hy + 4, 0.0D).color(255, 255, 255, 255).uv(1F, 1F).endVertex();
		buffer.vertex(hx + 4, hy - 4, 0.0D).color(255, 255, 255, 255).uv(1F, 0F).endVertex();
		buffer.vertex(hx - 4, hy - 4, 0.0D).color(255, 255, 255, 255).uv(0F, 0F).endVertex();
		tessellator.end();

		SendGeneralDataPacket d = FTBChunksClient.generalData;

		if (d != null) {
			List<Component> list = new ArrayList<>(4);
			list.add(new TranslatableComponent("ftbchunks.gui.claimed"));
			list.add(new TextComponent(d.claimed + " / " + d.maxClaimed).withStyle(d.claimed > d.maxClaimed ? ChatFormatting.RED : d.claimed == d.maxClaimed ? ChatFormatting.YELLOW : ChatFormatting.GREEN));
			list.add(new TranslatableComponent("ftbchunks.gui.force_loaded"));
			list.add(new TextComponent(d.loaded + " / " + d.maxLoaded).withStyle(d.loaded > d.maxLoaded ? ChatFormatting.RED : d.loaded == d.maxLoaded ? ChatFormatting.YELLOW : ChatFormatting.GREEN));

			for (int i = 0; i < list.size(); i++) {
				theme.drawString(matrixStack, list.get(i), 3, getScreen().getGuiScaledHeight() - 10 * (list.size() - i) - 1, Color4I.WHITE, Theme.SHADOW);
			}
		}
	}
}