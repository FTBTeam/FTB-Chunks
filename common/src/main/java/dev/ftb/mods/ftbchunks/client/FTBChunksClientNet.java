package dev.ftb.mods.ftbchunks.client;

import com.mojang.blaze3d.platform.Window;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.map.*;
import dev.ftb.mods.ftbchunks.client.map.color.ColorUtils;
import dev.ftb.mods.ftbchunks.config.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.config.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.data.ChunkSyncInfo;
import dev.ftb.mods.ftbchunks.net.PartialPackets;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;

public class FTBChunksClientNet {
    public static void handleSendChunkPacket(ResourceKey<Level> dimId, UUID teamId, Collection<ChunkSyncInfo> chunkSyncInfoList) {
        MapManager.getInstance().ifPresent(manager -> {
            MapDimension dimension = manager.getDimension(dimId);
            Date now = new Date();
            chunkSyncInfoList.forEach(chunkSyncInfo -> ClientTaskQueue.queue(new UpdateChunkFromServerTask(dimension, chunkSyncInfo, teamId, now)));
        });
    }

    public static void handleSyncRegionFromServer(RegionSyncKey key, int offset, int total, byte[] data) {
        PartialPackets.REGION.read(key, offset, total, data);
    }

    public static void handlePlayerDeathPacket(GlobalPos pos, int deathNumber) {
        if (FTBChunksClientConfig.DEATH_WAYPOINTS.get() && FTBChunksWorldConfig.playerHasMapStage(ClientUtils.getClientPlayer())) {
            MapManager.getInstance().ifPresent(manager -> {
                MapDimension dimension = manager.getDimension(pos.dimension());
                for (WaypointImpl w : dimension.getWaypointManager()) {
                    if (w.isDeathpoint()) {
                        w.setHidden(true);
                        w.refreshIcon();
                    }
                }

                WaypointImpl deathPoint = new WaypointImpl(WaypointType.DEATH, dimension, pos.pos())
                        .setName("Death #" + deathNumber)
                        .setColor(0xFF0000);
                dimension.getWaypointManager().add(deathPoint);
            });
        }
    }

    public static void handleBlockColorRequestPacket() {
        Minecraft mc = Minecraft.getInstance();

        mc.submit(() -> {
            mc.setScreen(null);

            new Thread(() -> {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mc.submit(() -> {
                    if (mc.hitResult instanceof BlockHitResult hitResult && mc.level != null && mc.player != null) {
                        Identifier id = BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(hitResult.getBlockPos()).getBlock());
                        Window window = mc.getWindow();
                        Screenshot.takeScreenshot(mc.getMainRenderTarget(), image -> {
                            int col = image.getPixel(image.getWidth() / 2 - (int) (2D * window.getGuiScale()), image.getHeight() / 2 - (int) (2D * window.getGuiScale()));
                            String s = String.format("\"%s\": \"#%06X\"", id.getPath(), ColorUtils.convertFromNative(col) & 0xFFFFFF);
                            mc.player.sendSystemMessage(Component.literal(id.getNamespace() + " - " + s)
                                    .withStyle(Style.EMPTY.applyFormat(ChatFormatting.GOLD)
                                            .withClickEvent(new ClickEvent.CopyToClipboard(s))
                                            .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))
                                    ));
                        });
                    }
                });
            }, "Color getter").start();
        });
    }

    public static void handleLoadedChunkViewPacket(ResourceKey<Level> dimension, Long2IntMap chunks) {
        MapManager.getInstance().ifPresent(manager -> {
            manager.getDimension(dimension).updateLoadedChunkView(chunks);
            manager.updateAllRegions(false);
        });
    }

    public static void handleLoginDataPacket(UUID serverId) {
        FTBChunks.LOGGER.info("Loading FTB Chunks client data from world {}", serverId);
        MapManager.startUp(serverId);
        FTBChunksClient.INSTANCE.getMinimapRenderer().requestTextureRefresh();
        FTBChunksClient.INSTANCE.getRerenderTracker().clear();
        ChunkUpdateTask.init();
    }

    public static void handleVanillaPacket(ClientboundSectionBlocksUpdatePacket p) {
        for (short position : p.positions) {
            FTBChunksClient.INSTANCE.getRerenderTracker().requestRerender(p.sectionPos.relativeToBlockPos(position));
        }
    }

    public static void handleVanillaPacket(ClientboundLevelChunkWithLightPacket p) {
        Level level = Minecraft.getInstance().level;
        if (level != null && level.hasChunk(p.getX(), p.getZ())) {
            FTBChunksClient.INSTANCE.getRerenderTracker().requestRerender(new ChunkPos(p.getX(), p.getZ()));
        }
    }

    public static void handleVanillaPacket(ClientboundBlockUpdatePacket p) {
        FTBChunksClient.INSTANCE.getRerenderTracker().requestRerender(p.getPos());
    }
}
