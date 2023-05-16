package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.data.ClaimResults;
import dev.ftb.mods.ftbchunks.net.RequestChunkChangePacket;
import dev.ftb.mods.ftbchunks.net.RequestMapDataPacket;
import dev.ftb.mods.ftbchunks.net.SendGeneralDataPacket;
import dev.ftb.mods.ftbchunks.net.UpdateForceLoadExpiryPacket;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.FaceIcon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ImageIcon;
import dev.ftb.mods.ftblibrary.math.MathUtils;
import dev.ftb.mods.ftblibrary.math.XZ;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TimeUtils;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.*;

import static dev.ftb.mods.ftbchunks.net.RequestChunkChangePacket.*;

/**
 * @author LatvianModder
 */
public class ChunkScreen extends BaseScreen {
	private class ChunkButton extends Button {
		private final XZ chunkPos;
		private final MapChunk chunk;
		private long lastAdjust = 0L;

		public ChunkButton(Panel panel, XZ xz) {
			super(panel, Component.empty(), Color4I.EMPTY);
			setSize(FTBChunks.TILE_SIZE, FTBChunks.TILE_SIZE);
			chunkPos = xz;
			chunk = dimension.getRegion(XZ.regionFromChunk(chunkPos.x, chunkPos.z)).getDataBlocking().getChunk(chunkPos);
		}

		@Override
		public void onClicked(MouseButton mouseButton) {
			selectedChunks.add(chunkPos);
		}

		@Override
		public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
			if (chunk.forceLoadedDate != null && !InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_TAB)) {
				Color4I teamColor = chunk.team.properties.get(Team.COLOR);
				float[] hsb = Color4I.RGBtoHSB(teamColor.redi(), teamColor.greeni(), teamColor.bluei(), null);
				hsb[0] = (hsb[0] + 0.5f) % 1f;
				FORCE_LOAD_ICON.withColor(Color4I.hsb(hsb[0], hsb[1], hsb[2])).draw(matrixStack, x, y, FTBChunks.TILE_SIZE, FTBChunks.TILE_SIZE);
			}
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
			if (chunk != null && chunk.getTeam() != null) {
				list.add(chunk.getTeam().getName().copy().withStyle(ChatFormatting.WHITE));

				Date date = new Date();

				if (Screen.hasAltDown()) {
					list.add(Component.literal(chunk.claimedDate.toLocaleString()).withStyle(ChatFormatting.GRAY));
				} else {
					list.add(Component.literal(TimeUtils.prettyTimeString((date.getTime() - chunk.claimedDate.getTime()) / 1000L) + " ago").withStyle(ChatFormatting.GRAY));
				}

				if (chunk.forceLoadedDate != null) {
					list.add(Component.translatable("ftbchunks.gui.force_loaded").withStyle(ChatFormatting.YELLOW));

					String loadStr = Screen.hasAltDown() ?
							"  " + chunk.forceLoadedDate.toLocaleString() :
							"  " + TimeUtils.prettyTimeString((date.getTime() - chunk.forceLoadedDate.getTime()) / 1000L) + " ago";
					list.add(Component.literal(loadStr).withStyle(ChatFormatting.GRAY));

					if (chunk.expiryDate != null) {
						list.add(Component.translatable("ftbchunks.gui.force_load_expires").withStyle(ChatFormatting.GOLD));
						String expireStr = Screen.hasAltDown() ?
								"  " + chunk.expiryDate.toLocaleString() :
								"  " + TimeUtils.prettyTimeString((chunk.expiryDate.getTime() - date.getTime()) / 1000L) + " from now";
						list.add(Component.literal(expireStr).withStyle(ChatFormatting.GRAY));
					}

					if (!Screen.hasAltDown()) {
						list.add(Component.translatable("ftbchunks.gui.hold_alt_for_dates").withStyle(ChatFormatting.DARK_GRAY));
					}
					if (chunk.team.isMember(Minecraft.getInstance().player.getUUID())){
						list.add(Component.translatable("ftbchunks.gui.mouse_wheel_expiry").withStyle(ChatFormatting.DARK_GRAY));
					}
				}
			}
		}

		@Override
		public boolean mouseScrolled(double scroll) {
			if (isMouseOver && chunk.forceLoadedDate != null && chunk.team.isMember(Minecraft.getInstance().player.getUUID())) {
				int dir = (int) Math.signum(scroll);
				long now = System.currentTimeMillis();
				if (chunk.expiryDate == null) chunk.expiryDate = new Date(now);
				long offset = (chunk.expiryDate.getTime() - now) / 1000L;
				if (dir == 1) {
					if (offset < 86400L) {
						offset = offset + 3600L;  // hour
					} else if (offset < 604800L) {
						offset = offset + 86400L;  // day
					} else {
						offset = offset + 604800L;  // week
					}
				} else if (dir == -1) {
					if (offset <= 86400L) {
						offset = Math.max(0L, offset - 3600L);
					} else if (offset <= 604800L) {
						offset = Math.max(86400L, offset - 86400L);
					} else {
						offset = Math.max(604800L, offset - 604800L);
					}
				}
				chunk.expiryDate = offset == 0L ? null : new Date(now + offset * 1000);
				lastAdjust = now;
				return true;
			}
			return super.mouseScrolled(scroll);
		}

		@Override
		public void tick() {
			super.tick();

			if (lastAdjust > 0L && System.currentTimeMillis() - lastAdjust > 1000L) {
				// send update to server, but not more than once a second - avoid flood of updates while adjusting mouse wheel
				new UpdateForceLoadExpiryPacket(chunkPos.dim(Minecraft.getInstance().level), chunk.expiryDate).sendToServer();
				lastAdjust = 0L;
			}
		}
	}

	private static final ImageIcon FORCE_LOAD_ICON = new ImageIcon(new ResourceLocation(FTBChunks.MOD_ID, "textures/force_loaded.png"));

	public MapDimension dimension = MapDimension.getCurrent();
	public List<ChunkButton> chunkButtons;
	public Set<XZ> selectedChunks;
	private ChunkUpdateInfo updateInfo;

	public ChunkScreen() {
		FTBChunksClient.alwaysRenderChunksOnMap = true;

		if (dimension == null) {
			FTBChunks.LOGGER.warn("Closed chunk screen map screen to prevent map dimension manager crash");
			this.closeGui();
		}

		if (MapManager.inst != null) {
			MapManager.inst.updateAllRegions(false);
		}
	}

	public static void notifyChunkUpdates(int totalChunks, int changedChunks, EnumMap<ClaimResults, Integer> problems) {
		if (Minecraft.getInstance().screen instanceof ScreenWrapper sw && sw.getGui() instanceof ChunkScreen cs) {
			cs.updateInfo = new ChunkUpdateInfo(totalChunks, changedChunks, problems, Minecraft.getInstance().level.getGameTime());
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
		ChunkPos chunkPos = player.chunkPosition();
		int startX = chunkPos.x - FTBChunks.TILE_OFFSET;
		int startZ = chunkPos.z - FTBChunks.TILE_OFFSET;

		chunkButtons = new ArrayList<>();
		selectedChunks = new LinkedHashSet<>();

		for (int z = 0; z < FTBChunks.TILES; z++) {
			for (int x = 0; x < FTBChunks.TILES; x++) {
				ChunkButton button = new ChunkButton(this, XZ.of(startX + x, startZ + z));
				chunkButtons.add(button);
				button.setPos(sx + x * FTBChunks.TILE_SIZE, sy + z * FTBChunks.TILE_SIZE);
			}
		}

		addAll(chunkButtons);
		new RequestMapDataPacket(chunkPos.x - FTBChunks.TILE_OFFSET, chunkPos.z - FTBChunks.TILE_OFFSET, chunkPos.x + FTBChunks.TILE_OFFSET, chunkPos.z + FTBChunks.TILE_OFFSET).sendToServer();
		add(new SimpleButton(this, Component.translatable("ftbchunks.gui.large_map"), Icons.MAP, (simpleButton, mouseButton) -> new LargeMapScreen().openGui()).setPosAndSize(1, 1, 16, 16));
	}

	@Override
	public void mouseReleased(MouseButton button) {
		super.mouseReleased(button);

		if (!selectedChunks.isEmpty()) {
			new RequestChunkChangePacket(isShiftKeyDown() ? (button.isLeft() ? LOAD : UNLOAD) : (button.isLeft() ? CLAIM : UNCLAIM), selectedChunks).sendToServer();
			selectedChunks.clear();
			playClickSound();
		}
	}

	@Override
	public boolean keyPressed(Key key) {
		if (FTBChunksWorldConfig.playerHasMapStage(Minecraft.getInstance().player) && (key.is(GLFW.GLFW_KEY_M) || key.is(GLFW.GLFW_KEY_C))) {
			new LargeMapScreen().openGui();
			return true;
		}

		return super.keyPressed(key);
	}

	@Override
	public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		Player player = Minecraft.getInstance().player;

		int sx = x + (w - FTBChunks.MINIMAP_SIZE) / 2;
		int sy = y + (h - FTBChunks.MINIMAP_SIZE) / 2;

		int r = 70;
		int g = 70;
		int b = 70;
		int a = 100;

		RenderSystem.lineWidth(Math.max(2.5F, (float) Minecraft.getInstance().getWindow().getWidth() / 1920.0F * 2.5F));

		RenderSystem.enableTexture();
		RenderSystem.bindTextureForSetup(FTBChunksClient.minimapTextureId);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		RenderSystem.setShaderTexture(0, FTBChunksClient.minimapTextureId);
		GuiHelper.drawTexturedRect(matrixStack, sx, sy, FTBChunks.MINIMAP_SIZE, FTBChunks.MINIMAP_SIZE, Color4I.WHITE, 0F, 0F, 1F, 1F);

		if (!InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_TAB)) {
			Tesselator tessellator = Tesselator.getInstance();
			BufferBuilder buffer = tessellator.getBuilder();

			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
			RenderSystem.disableTexture();
			buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
			Matrix4f m = matrixStack.last().pose();

			for (int gy = 1; gy < FTBChunks.TILES; gy++) {
				buffer.vertex(m, sx, sy + gy * FTBChunks.TILE_SIZE, 0).color(r, g, b, a).endVertex();
				buffer.vertex(m, sx + FTBChunks.MINIMAP_SIZE, sy + gy * FTBChunks.TILE_SIZE, 0).color(r, g, b, a).endVertex();
			}

			for (int gx = 1; gx < FTBChunks.TILES; gx++) {
				buffer.vertex(m, sx + gx * FTBChunks.TILE_SIZE, sy, 0).color(r, g, b, a).endVertex();
				buffer.vertex(m, sx + gx * FTBChunks.TILE_SIZE, sy + FTBChunks.MINIMAP_SIZE, 0).color(r, g, b, a).endVertex();
			}

			tessellator.end();
		}

		RenderSystem.enableTexture();
		RenderSystem.lineWidth(1F);

		double hx = sx + FTBChunks.TILE_SIZE * FTBChunks.TILE_OFFSET + MathUtils.mod(player.getX(), 16D);
		double hy = sy + FTBChunks.TILE_SIZE * FTBChunks.TILE_OFFSET + MathUtils.mod(player.getZ(), 16D);
		FaceIcon.getFace(player.getGameProfile()).draw(matrixStack, (int) (hx - 4D), (int) (hy - 4D), 8, 8);

		SendGeneralDataPacket d = FTBChunksClient.generalData;

		if (d != null) {
			List<Component> list = new ArrayList<>(4);
			list.add(Component.translatable("ftbchunks.gui.claimed"));
			list.add(Component.literal(d.claimed + " / " + d.maxClaimChunks).withStyle(d.claimed > d.maxClaimChunks ? ChatFormatting.RED : d.claimed == d.maxClaimChunks ? ChatFormatting.YELLOW : ChatFormatting.GREEN));
			list.add(Component.translatable("ftbchunks.gui.force_loaded"));
			list.add(Component.literal(d.loaded + " / " + d.maxForceLoadChunks).withStyle(d.loaded > d.maxForceLoadChunks ? ChatFormatting.RED : d.loaded == d.maxForceLoadChunks ? ChatFormatting.YELLOW : ChatFormatting.GREEN));

			int fh = theme.getFontHeight() + 1;
			for (int i = 0; i < list.size(); i++) {
				theme.drawString(matrixStack, list.get(i), 3, getScreen().getGuiScaledHeight() - fh * (list.size() - i) - 1, Color4I.WHITE, Theme.SHADOW);
			}
		}

		if (updateInfo != null && updateInfo.shouldDisplay()) {
			theme.drawString(matrixStack, updateInfo.summary(), sx + 2, sy + 2, Theme.SHADOW);
			int line = 1;
			for (Map.Entry<ClaimResults, Integer> entry : updateInfo.problems.entrySet()) {
				ClaimResults problem = entry.getKey();
				int count = entry.getValue();
				theme.drawString(matrixStack, Component.translatable(problem.getTranslationKey()).append(": " + count), sx + 2, sy + 5 + theme.getFontHeight() * line++, Theme.SHADOW);
			}
		}
	}

	private record ChunkUpdateInfo(int totalChunks, int changedChunks, EnumMap<ClaimResults, Integer> problems, long timestamp) {
		public boolean shouldDisplay() {
			// display for 3 seconds + 1 second per line of problem data
			long timeout = 60L + 20L * problems.size();
			return Minecraft.getInstance().level.getGameTime() - timestamp < timeout;
		}

		public Component summary() {
			ChatFormatting color = changedChunks == 0 ? ChatFormatting.RED : (changedChunks < totalChunks ? ChatFormatting.YELLOW : ChatFormatting.GREEN);
			return Component.translatable("ftbchunks.claim_result", changedChunks, totalChunks).withStyle(ChatFormatting.UNDERLINE, color);
		}
	}
}
