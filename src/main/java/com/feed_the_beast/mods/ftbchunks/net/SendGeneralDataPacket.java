package com.feed_the_beast.mods.ftbchunks.net;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import com.feed_the_beast.mods.ftbchunks.FTBChunksConfig;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunk;
import com.feed_the_beast.mods.ftbchunks.api.ClaimedChunkPlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * @author LatvianModder
 */
public class SendGeneralDataPacket {
	public static void send(ClaimedChunkPlayerData playerData, ServerPlayer player) {
		SendGeneralDataPacket data = new SendGeneralDataPacket();

		data.maxClaimed = FTBChunksConfig.getMaxClaimedChunks(playerData, player);
		data.maxLoaded = FTBChunksConfig.getMaxForceLoadedChunks(playerData, player);

		for (ClaimedChunk chunk : playerData.getClaimedChunks()) {
			data.claimed++;

			if (chunk.isForceLoaded()) {
				data.loaded++;
			}
		}

		FTBChunksNet.MAIN.send(PacketDistributor.PLAYER.with(() -> player), data);
	}

	public int claimed;
	public int loaded;
	public int maxClaimed;
	public int maxLoaded;

	public SendGeneralDataPacket() {
	}

	SendGeneralDataPacket(FriendlyByteBuf buf) {
		claimed = buf.readVarInt();
		loaded = buf.readVarInt();
		maxClaimed = buf.readVarInt();
		maxLoaded = buf.readVarInt();
	}

	void write(FriendlyByteBuf buf) {
		buf.writeVarInt(claimed);
		buf.writeVarInt(loaded);
		buf.writeVarInt(maxClaimed);
		buf.writeVarInt(maxLoaded);
	}

	void handle(Supplier<NetworkEvent.Context> context) {
		context.get().enqueueWork(() -> FTBChunks.instance.proxy.updateGeneralData(this));
		context.get().setPacketHandled(true);
	}
}