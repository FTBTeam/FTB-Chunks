package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Inject(method = "handleChunkBlocksUpdate", at = @At("RETURN"))
	public void handleChunkBlocksUpdateFTBC(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
		packet.runUpdates((blockPos, blockState) -> FTBChunksClient.rerender(blockPos));
	}

	@Inject(method = "handleLevelChunk", at = @At("RETURN"))
	public void handleLevelChunkFTBC(ClientboundLevelChunkPacket packet, CallbackInfo ci) {
		FTBChunksClient.rerender(new ChunkPos(packet.getX(), packet.getZ()));
	}

	@Inject(method = "handleBlockUpdate", at = @At("RETURN"))
	public void handleBlockUpdateFTBC(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
		FTBChunksClient.rerender(packet.getPos());
	}
}
