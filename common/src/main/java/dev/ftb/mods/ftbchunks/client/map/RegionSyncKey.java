package dev.ftb.mods.ftbchunks.client.map;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record RegionSyncKey(ResourceKey<Level> dim, int x, int z, int random) {
	public static StreamCodec<FriendlyByteBuf, RegionSyncKey> STREAM_CODEC = StreamCodec.composite(
			ResourceKey.streamCodec(Registries.DIMENSION), RegionSyncKey::dim,
			ByteBufCodecs.INT, RegionSyncKey::x,
			ByteBufCodecs.INT, RegionSyncKey::z,
			ByteBufCodecs.INT, RegionSyncKey::random,
			RegionSyncKey::new
	);
}