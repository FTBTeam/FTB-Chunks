package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.gui.ChunkScreen;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import net.minecraft.ChatFormatting;
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

            Optional<Team> teamByID = FTBTeamsAPI.api().getClientManager().getTeamByID(message.teamId);
            if (teamByID.isEmpty()) {
                player.sendSystemMessage(Component.translatable("ftbteams.team_not_found", message.teamId, ChatFormatting.RED));
                return;
            }

            ChunkScreen.openChunkScreen(teamByID.get());
        });
    }

}
