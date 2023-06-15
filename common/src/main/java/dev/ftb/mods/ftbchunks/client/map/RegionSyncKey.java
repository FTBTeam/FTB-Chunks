package dev.ftb.mods.ftbchunks.client.map;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * @author LatvianModder
 */
public class RegionSyncKey {
	private final ResourceKey<Level> dim;
	private final int x;
	private final int z;
	private final int random;

	public RegionSyncKey(FriendlyByteBuf buf) {
		dim = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
		x = buf.readVarInt();
		z = buf.readVarInt();
		random = buf.readInt();
	}

	public RegionSyncKey(ResourceKey<Level> dim, int x, int z, int random) {
		this.dim = dim;
		this.x = x;
		this.z = z;
		this.random = random;
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(dim.location());
		buf.writeVarInt(x);
		buf.writeVarInt(z);
		buf.writeInt(random);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RegionSyncKey key = (RegionSyncKey) o;
		return x == key.x &&
				z == key.z &&
				random == key.random &&
				dim.equals(key.dim);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dim, x, z, random);
	}
}