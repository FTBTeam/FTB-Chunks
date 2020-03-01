package com.feed_the_beast.mods.ftbchunks;

import com.feed_the_beast.mods.ftbteams.api.FTBTeamsAPI;
import com.feed_the_beast.mods.ftbteams.api.Team;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class FTBTeamsIntegration
{
	public static boolean isAlly(ClaimedChunkPlayerData data, ServerPlayerEntity player)
	{
		Optional<Team> team1 = FTBTeamsAPI.INSTANCE.getManager().getTeam(new GameProfile(data.getUuid(), data.getName()));

		if (team1.isPresent())
		{
			Optional<Team> team2 = FTBTeamsAPI.INSTANCE.getManager().getTeam(player);
			return team2.isPresent() && team1.get().equals(team2.get());
		}

		return false;
	}
}