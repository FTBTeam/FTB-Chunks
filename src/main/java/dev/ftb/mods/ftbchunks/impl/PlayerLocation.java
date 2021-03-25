package dev.ftb.mods.ftbchunks.impl;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public final class PlayerLocation {
	public static ResourceKey<Level> currentDimension = null;
	public static final ArrayList<PlayerLocation> CLIENT_LIST = new ArrayList<>();

	public String name;
	public UUID uuid;
	public int x;
	public int z;

	public PlayerLocation() {
	}

	public PlayerLocation(Player player) {
		name = player.getGameProfile().getName();
		uuid = player.getUUID();
		x = Mth.floor(player.getX());
		z = Mth.floor(player.getZ());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		PlayerLocation that = (PlayerLocation) o;
		return x == that.x &&
				z == that.z &&
				name.equals(that.name) &&
				uuid.equals(that.uuid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, uuid, x, z);
	}
}