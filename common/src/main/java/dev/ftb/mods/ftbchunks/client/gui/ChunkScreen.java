package dev.ftb.mods.ftbchunks.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.client.icon.MapType;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.RenderMapImageTask;
import dev.ftb.mods.ftbchunks.net.RequestChunkChangePacket;
import dev.ftb.mods.ftbchunks.net.RequestMapDataPacket;
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
import dev.ftb.mods.ftblibrary.ui.misc.KeyReferenceScreen;
import dev.ftb.mods.ftblibrary.util.TimeUtils;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static dev.ftb.mods.ftbchunks.net.RequestChunkChangePacket.ChunkChangeOp;

public class ChunkScreen extends BaseScreen {
	private static final ImageIcon FORCE_LOAD_ICON = new ImageIcon(FTBChunksAPI.rl("textures/force_loaded.png"));

	private final MapDimension dimension;
	private final List<ChunkButton> chunkButtons = new ArrayList<>();
	private final Set<XZ> selectedChunks = new HashSet<>();
	@Nullable
	private final Team openedAs;
	private ChunkUpdateInfo updateInfo;
	private boolean isAdminEnabled;

	public ChunkScreen(MapDimension dimension, @Nullable Team openedAs) {
		RenderMapImageTask.setAlwaysRenderChunksOnMap(true);

		this.isAdminEnabled = Minecraft.getInstance().isSingleplayer();
		this.dimension = dimension;
		this.openedAs = openedAs;

		MapManager.getInstance().ifPresent(m -> m.updateAllRegions(false));
	}

	public static void openChunkScreen(@Nullable Team openedAs) {
		MapDimension.getCurrent().ifPresentOrElse(
				mapDimension -> new ChunkScreen(mapDimension, openedAs).openGui(),
				() -> FTBChunks.LOGGER.warn("MapDimension data missing?? not opening chunk screen")
		);
	}

	public static void openChunkScreen() {
		openChunkScreen(null);
	}


	public static void notifyChunkUpdates(int totalChunks, int changedChunks, EnumMap<ClaimResult.StandardProblem, Integer> problems) {
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
		RenderMapImageTask.setAlwaysRenderChunksOnMap(false);

		MapManager.getInstance().ifPresent(m -> m.updateAllRegions(false));

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

		chunkButtons.clear();
		for (int z = 0; z < FTBChunks.TILES; z++) {
			for (int x = 0; x < FTBChunks.TILES; x++) {
				ChunkButton button = new ChunkButton(this, XZ.of(startX + x, startZ + z));
				chunkButtons.add(button);
				button.setPos(sx + x * FTBChunks.TILE_SIZE, sy + z * FTBChunks.TILE_SIZE);
			}
		}

		addAll(chunkButtons);
		NetworkManager.sendToServer(new RequestMapDataPacket(
				chunkPos.x - FTBChunks.TILE_OFFSET, chunkPos.z - FTBChunks.TILE_OFFSET,
				chunkPos.x + FTBChunks.TILE_OFFSET, chunkPos.z + FTBChunks.TILE_OFFSET)
		);

		add(new SimpleButton(this, Component.translatable("ftbchunks.gui.large_map"), Icons.MAP,
				(simpleButton, mouseButton) -> LargeMapScreen.openMap()
		).setPosAndSize(1, 1, 16, 16));

		add(new SimpleButton(this, Component.translatable("ftbchunks.gui.chunk_info"), Icons.INFO,
				(btn, mb) -> new ChunkMouseReferenceScreen().openGui()
		).setPosAndSize(1, 19, 16, 16));

        if (!Minecraft.getInstance().isSingleplayer() && openedAs == null && Minecraft.getInstance().player.hasPermissions(Commands.LEVEL_GAMEMASTERS)) {
            add(new AdminButton().setPosAndSize(1, 37, 16, 16));
        }
	}

	@Override
	public void mouseReleased(MouseButton button) {
		super.mouseReleased(button);

		if (!selectedChunks.isEmpty()) {
			Optional<UUID> teamId = Optional.ofNullable(openedAs).map(Team::getTeamId);
			NetworkManager.sendToServer(new RequestChunkChangePacket(ChunkChangeOp.create(button.isLeft(), isShiftKeyDown()), selectedChunks, canChangeAsAdmin(), teamId));
			selectedChunks.clear();
			playClickSound();
		}
	}

	@Override
	public boolean keyPressed(Key key) {
		if (FTBChunksWorldConfig.playerHasMapStage(Minecraft.getInstance().player) && (key.is(GLFW.GLFW_KEY_M) || key.is(GLFW.GLFW_KEY_C))) {
			LargeMapScreen.openMap();
			return true;
		}

		return super.keyPressed(key);
	}

	@Override
	public void drawBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
		Player player = Minecraft.getInstance().player;

		int sx = x + (w - FTBChunks.MINIMAP_SIZE) / 2;
		int sy = y + (h - FTBChunks.MINIMAP_SIZE) / 2;

		RenderSystem.setShaderTexture(0, FTBChunksClient.INSTANCE.getMinimapTextureId());
		GuiHelper.drawTexturedRect(graphics, sx, sy, FTBChunks.MINIMAP_SIZE, FTBChunks.MINIMAP_SIZE, Color4I.WHITE, 0F, 0F, 1F, 1F);

		if (!InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_TAB)) {
			for (int gy = 1; gy < FTBChunks.TILES; gy++) {
				graphics.hLine(sx, sx + FTBChunks.MINIMAP_SIZE, sy +  gy * FTBChunks.TILE_SIZE, 0x64464646);
			}
			for (int gx = 1; gx < FTBChunks.TILES; gx++) {
				graphics.vLine(sx + gx * FTBChunks.TILE_SIZE, sy, sy + FTBChunks.MINIMAP_SIZE, 0x64464646);
			}
		}

		double hx = sx + FTBChunks.TILE_SIZE * FTBChunks.TILE_OFFSET + MathUtils.mod(player.getX(), 16D);
		double hy = sy + FTBChunks.TILE_SIZE * FTBChunks.TILE_OFFSET + MathUtils.mod(player.getZ(), 16D);
		new PointerIcon().draw(MapType.LARGE_MAP, graphics, (int) (hx - 4D), (int) (hy - 4D), 8, 8, false, 255);
		FaceIcon.getFace(player.getGameProfile()).draw(graphics, (int) (hx - 4D), (int) (hy - 4D), 8, 8);

		List<Component> list = FTBChunksClient.INSTANCE.getChunkSummary();
		int fh = theme.getFontHeight() + 1;
		for (int i = 0; i < list.size(); i++) {
			theme.drawString(graphics, list.get(i), 3, getScreen().getGuiScaledHeight() - fh * (list.size() - i) - 1, Color4I.WHITE, Theme.SHADOW);
		}

		if (updateInfo != null && updateInfo.shouldDisplay()) {
			theme.drawString(graphics, updateInfo.summary(), sx + 2, sy + 2, Theme.SHADOW);
			int line = 1;
			for (Map.Entry<ClaimResult.StandardProblem, Integer> entry : updateInfo.problems.entrySet()) {
				ClaimResult.StandardProblem problem = entry.getKey();
				int count = entry.getValue();
				theme.drawString(graphics, problem.getMessage().append(": " + count), sx + 2, sy + 5 + theme.getFontHeight() * line++, Theme.SHADOW);
			}
		}

		if(openedAs != null) {
			String openAsMessage = Component.translatable("ftbchunks.gui.opened_as", openedAs.getName()).getString();
			theme.drawString(graphics, openAsMessage, sx + 2, sy + FTBChunks.MINIMAP_SIZE - theme.getFontHeight() - 2, Color4I.WHITE, Theme.SHADOW);
		}
	}

	private boolean canChangeAsAdmin() {
		return Minecraft.getInstance().player.hasPermissions(Commands.LEVEL_GAMEMASTERS) && openedAs == null && isAdminEnabled;
	}

	private static class ChunkMouseReferenceScreen extends KeyReferenceScreen {
		public ChunkMouseReferenceScreen() {
			super("ftbchunks.gui.chunk_info.text");
		}

		@Override
		public Component getTitle() {
			return Component.translatable("ftbchunks.gui.chunk_info");
		}
	}

	private class ChunkButton extends Button {
		private final XZ chunkPos;
		private final MapChunk chunk;
		private long lastAdjust = 0L;

		public ChunkButton(Panel panel, XZ xz) {
			super(panel, Component.empty(), Color4I.empty());
			setSize(FTBChunks.TILE_SIZE, FTBChunks.TILE_SIZE);
			chunkPos = xz;
			chunk = dimension.getRegion(XZ.regionFromChunk(chunkPos.x(), chunkPos.z())).getDataBlocking().getChunk(chunkPos);
		}

		@Override
		public void onClicked(MouseButton mouseButton) {
			selectedChunks.add(chunkPos);
		}

		@Override
		public void drawBackground(GuiGraphics matrixStack, Theme theme, int x, int y, int w, int h) {
			if (chunk.getForceLoadedDate().isPresent() && !InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_TAB)) {
				chunk.getTeam().ifPresent(team -> {
					Color4I teamColor = team.getProperties().get(TeamProperties.COLOR);
					float[] hsb = Color4I.RGBtoHSB(teamColor.redi(), teamColor.greeni(), teamColor.bluei(), null);
					hsb[0] = (hsb[0] + 0.5f) % 1f;
					FORCE_LOAD_ICON.withColor(Color4I.hsb(hsb[0], hsb[1], hsb[2])).draw(matrixStack, x, y, FTBChunks.TILE_SIZE, FTBChunks.TILE_SIZE);
				});
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
			if (chunk == null) {
				return;
			}
			chunk.getTeam().ifPresent(team -> {
				list.add(team.getName().copy().withStyle(ChatFormatting.WHITE));

				Date date = new Date();

				chunk.getClaimedDate().ifPresent(claimedDate -> {
					if (Screen.hasAltDown()) {
						list.add(Component.literal(claimedDate.toLocaleString()).withStyle(ChatFormatting.GRAY));
					} else {
						list.add(Component.literal(TimeUtils.prettyTimeString((date.getTime() - claimedDate.getTime()) / 1000L) + " ago").withStyle(ChatFormatting.GRAY));
					}
				});

				chunk.getForceLoadedDate().ifPresent(forceLoadedDate -> {
					list.add(Component.translatable("ftbchunks.gui.force_loaded").withStyle(ChatFormatting.YELLOW));

					String loadStr = Screen.hasAltDown() ?
							"  " + forceLoadedDate.toLocaleString() :
							"  " + TimeUtils.prettyTimeString((date.getTime() - forceLoadedDate.getTime()) / 1000L) + " ago";
					list.add(Component.literal(loadStr).withStyle(ChatFormatting.GRAY));

					chunk.getForceLoadExpiryDate().ifPresent(expiryDate -> {
						list.add(Component.translatable("ftbchunks.gui.force_load_expires").withStyle(ChatFormatting.GOLD));
						String expireStr = Screen.hasAltDown() ?
								"  " + expiryDate.toLocaleString() :
								"  " + TimeUtils.prettyTimeString((expiryDate.getTime() - date.getTime()) / 1000L) + " from now";
						list.add(Component.literal(expireStr).withStyle(ChatFormatting.GRAY));
					});

					if (!Screen.hasAltDown()) {
						list.add(Component.translatable("ftbchunks.gui.hold_alt_for_dates").withStyle(ChatFormatting.DARK_GRAY));
					}
					if (team.getRankForPlayer(Minecraft.getInstance().player.getUUID()).isMemberOrBetter()){
						list.add(Component.translatable("ftbchunks.gui.mouse_wheel_expiry").withStyle(ChatFormatting.DARK_GRAY));
					}
				});
			});
		}

		@Override
		public boolean mouseScrolled(double scroll) {
			return chunk.getForceLoadedDate().map(forceLoadedDate -> {
				LocalPlayer player = Minecraft.getInstance().player;
				boolean teamMember = chunk.isTeamMember(player);
				if (isMouseOver && (canChangeAsAdmin() || teamMember)) {
					int dir = (int) Math.signum(scroll);
					long now = System.currentTimeMillis();
					Date expiry = chunk.getForceLoadExpiryDate().orElse(new Date(now));
					long offset = calcOffset(expiry, now, dir);
					chunk.updateForceLoadExpiryDate(now, offset * 1000L);
					lastAdjust = now;
					return true;
				}
				return super.mouseScrolled(scroll);
			}).orElse(super.mouseScrolled(scroll));
		}

		private static long calcOffset(Date expiry, long now, int dir) {
			long offset = (expiry.getTime() - now) / 1000L;
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
			return offset;
		}

		@Override
		public void tick() {
			super.tick();

			if (lastAdjust > 0L && System.currentTimeMillis() - lastAdjust > 1000L) {
				// send update to server, but not more than once a second - avoid flood of updates while adjusting mouse wheel
				NetworkManager.sendToServer(new UpdateForceLoadExpiryPacket(chunkPos.dim(Minecraft.getInstance().level), chunk.getForceLoadExpiryDate().orElse(null)));
				lastAdjust = 0L;
			}
		}
	}

	private record ChunkUpdateInfo(int totalChunks, int changedChunks, EnumMap<ClaimResult.StandardProblem, Integer> problems, long timestamp) {
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

	private class AdminButton extends SimpleButton {

		private static final Component DISABLED = Component.translatable("ftbchunks.gui.admin_mode_disabled");
		private static final Component ENABLED = Component.translatable("ftbchunks.gui.admin_mode_enabled");
		private static final Component MORE_INFO = Component.translatable("ftbchunks.gui.admin_mode_info", ChatFormatting.GRAY);

		public AdminButton() {
			super(ChunkScreen.this, DISABLED, Icons.CANCEL, (btn, mb) -> {
				ChunkScreen.this.isAdminEnabled = !ChunkScreen.this.isAdminEnabled;
				btn.setIcon(ChunkScreen.this.isAdminEnabled ? Icons.ACCEPT : Icons.CANCEL);
				btn.setTitle(ChunkScreen.this.isAdminEnabled ? DISABLED : ENABLED);
			});
		}

		@Override
		public void addMouseOverText(TooltipList list) {
			super.addMouseOverText(list);
			list.add(MORE_INFO);
		}
	}
}
