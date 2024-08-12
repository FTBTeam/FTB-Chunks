package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.gui.ChunkScreen;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public record OpenClaimGUIPacket(UUID teamId) implements CustomPacketPayload {

    public static final Type<OpenClaimGUIPacket> TYPE = new Type<>(FTBChunksAPI.rl("open_claim_gui_packet"));

    public static final StreamCodec<FriendlyByteBuf, OpenClaimGUIPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, OpenClaimGUIPacket::teamId,
            OpenClaimGUIPacket::new
    );

    @Override
    public Type<OpenClaimGUIPacket> type() {
        return TYPE;
    }

    public static void handle(OpenClaimGUIPacket message, NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            Optional<Team> teamByID = ClaimedChunkManagerImpl.getInstance().getTeamManager().getTeamByID(message.teamId);
            if (teamByID.isEmpty()) {
                player.sendSystemMessage(Component.nullToEmpty("Team not found!"));
                return;
            }
            ChunkScreen.openChunkScreen(teamByID.get());
        });
    }

//    private ChunkTeamData findChunkTeamData(Player serverPlayer, Optional<UUID> openAsPlayer, Optional<UUID> openAsTeam, boolean canOpenOthers) {
//        ChunkTeamData data = ClaimedChunkManagerImpl.getInstance().getPersonalData(serverPlayer.getUUID());
//        if(!canOpenOthers) {
//            return data;
//        }
//        if(openAsPlayer.isPresent() && openAsTeam.isPresent()) {
//            FTBChunks.LOGGER.warn("Player {} tried to open a chunk change menu as both a player and a team", serverPlayer.getScoreboardName());
//            return data;
//        }
//
//        if(openAsPlayer.isPresent()) {
//            UUID uuid = openAsPlayer.get();
//            return ClaimedChunkManagerImpl.getInstance().getPersonalData(uuid);
//        }
//
//        if(openAsTeam.isPresent()) {
//            UUID uuid = openAsTeam.get();
//            return ClaimedChunkManagerImpl.getInstance().getTeamManager().getTeamByID(uuid)
//                    .map(ClaimedChunkManagerImpl.getInstance()::getOrCreateData)
//                    .orElse(null);
//        }
//        return null;
//    }
}
