package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.ClaimVisibilityOverride;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public enum ChunkScreenClosed implements CustomPacketPayload {
    INSTANCE;

    public static final Type<ChunkScreenClosed> TYPE = new Type<>(FTBChunksAPI.rl("chunk_screen_closed"));
    public static final StreamCodec<FriendlyByteBuf, ChunkScreenClosed> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChunkScreenClosed ignored, NetworkManager.PacketContext context) {
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer sp) {
                UUID teamId = ClaimVisibilityOverride.INSTANCE.remove(sp);
                if (teamId != null) {
                    FTBTeamsAPI.api().getManager().getTeamByID(teamId)
                            .ifPresent(team -> ClaimedChunkManagerImpl.getInstance().getOrCreateData(team).syncChunksToPlayer(sp));
                }
            }
        });
    }
}
