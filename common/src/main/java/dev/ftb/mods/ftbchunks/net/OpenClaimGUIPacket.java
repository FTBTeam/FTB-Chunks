package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.gui.map.ChunkScreen;
import dev.ftb.mods.ftblibrary.platform.network.PacketContext;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record OpenClaimGUIPacket(UUID teamId) implements CustomPacketPayload {

    public static final Type<OpenClaimGUIPacket> TYPE = new Type<>(FTBChunksAPI.id("open_claim_gui_packet"));

    public static final StreamCodec<FriendlyByteBuf, OpenClaimGUIPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, OpenClaimGUIPacket::teamId,
            OpenClaimGUIPacket::new
    );

    @Override
    public Type<OpenClaimGUIPacket> type() {
        return TYPE;
    }

    public static void handle(OpenClaimGUIPacket message, PacketContext context) {
        context.enqueue(() ->
                FTBTeamsAPI.api().getClientManager().getTeamByID(message.teamId).ifPresentOrElse(
                        ChunkScreen::openChunkScreen,
                        () -> context.player().sendSystemMessage(Component.translatable(
                                "ftbteams.team_not_found", message.teamId, ChatFormatting.RED))
                ));
    }

}
