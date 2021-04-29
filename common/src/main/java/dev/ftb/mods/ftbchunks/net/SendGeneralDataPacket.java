package dev.ftb.mods.ftbchunks.net;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import me.shedaniel.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * @author LatvianModder
 */
public class SendGeneralDataPacket extends MessageBase {
	public static void send(FTBChunksTeamData playerData, ServerPlayer player) {
		SendGeneralDataPacket data = new SendGeneralDataPacket();

		data.maxClaimed = playerData.manager.config.getMaxClaimedChunks(playerData, player);
		data.maxLoaded = playerData.manager.config.getMaxForceLoadedChunks(playerData, player);

		for (ClaimedChunk chunk : playerData.getClaimedChunks()) {
			data.claimed++;

			if (chunk.isForceLoaded()) {
				data.loaded++;
			}
		}

		data.sendTo(player);
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

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeVarInt(claimed);
		buf.writeVarInt(loaded);
		buf.writeVarInt(maxClaimed);
		buf.writeVarInt(maxLoaded);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBChunks.PROXY.updateGeneralData(this);
	}
}