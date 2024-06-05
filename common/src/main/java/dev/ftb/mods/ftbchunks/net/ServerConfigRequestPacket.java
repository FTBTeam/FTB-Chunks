package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftbchunks.util.DimensionFilter;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.snbt.config.ConfigUtil;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public record ServerConfigRequestPacket(SNBTCompoundTag config) implements CustomPacketPayload {
    public static final Type<ServerConfigRequestPacket> TYPE = new Type<>(FTBChunksAPI.rl("server_config_request_packet"));

    public static final StreamCodec<FriendlyByteBuf, ServerConfigRequestPacket> STREAM_CODEC = StreamCodec.composite(
            SNBTCompoundTag.STREAM_CODEC, ServerConfigRequestPacket::config,
            ServerConfigRequestPacket::new
    );

    @Override
    public Type<ServerConfigRequestPacket> type() {
        return TYPE;
    }

    public static void handle(ServerConfigRequestPacket message, NetworkManager.PacketContext context) {
        if (context.getPlayer() instanceof ServerPlayer sp && sp.hasPermissions(2)) {
            context.queue(() -> {
                MinecraftServer server = sp.getServer();

                FTBChunks.LOGGER.info("FTB Chunks server config updated from client by player {}", sp.getName().getString());
                FTBChunksWorldConfig.CONFIG.read(message.config);

                DimensionFilter.clearMatcherCaches();

                Path file = server.getWorldPath(ConfigUtil.SERVER_CONFIG_DIR).resolve(FTBChunksWorldConfig.CONFIG.key + ".snbt");
                FTBChunksWorldConfig.CONFIG.save(file);

                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (!sp.getUUID().equals(player.getUUID())) {
                        NetworkManager.sendToPlayer(player, new ServerConfigResponsePacket(message.config));
                    }
                }

                FTBTeamsAPI.api().getManager().getTeams().forEach(team ->
                        ClaimedChunkManagerImpl.getInstance().getOrCreateData(team).updateLimits());
            });
        }
    }
}
