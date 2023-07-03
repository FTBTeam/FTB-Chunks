package dev.ftb.mods.ftbchunks.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;

public class PlayerDeathPacket extends BaseS2CMessage {
	private final GlobalPos pos;
	private final int number;

	public PlayerDeathPacket(GlobalPos pos, int num) {
		this.pos = pos;
		number = num;
	}

	PlayerDeathPacket(FriendlyByteBuf buf) {
		pos = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation()), buf.readBlockPos());
		number = buf.readVarInt();
	}

	@Override
	public MessageType getType() {
		return FTBChunksNet.PLAYER_DEATH;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(pos.dimension().location());
		buf.writeBlockPos(pos.pos());
		buf.writeVarInt(number);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunksClient.handlePlayerDeath(pos, number);
	}
}