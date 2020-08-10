package com.feed_the_beast.mods.ftbchunks.impl;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public final class PlayerLocation
{
	public static String currentDimension = "";
	public static final ArrayList<PlayerLocation> CLIENT_LIST = new ArrayList<>();

	public String name;
	public UUID uuid;
	public int x;
	public int z;

	public PlayerLocation()
	{
	}

	public PlayerLocation(PlayerEntity player)
	{
		name = player.getGameProfile().getName();
		uuid = player.getUniqueID();
		x = MathHelper.floor(player.getPosX());
		z = MathHelper.floor(player.getPosZ());
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		PlayerLocation that = (PlayerLocation) o;
		return x == that.x &&
				z == that.z &&
				name.equals(that.name) &&
				uuid.equals(that.uuid);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, uuid, x, z);
	}
}