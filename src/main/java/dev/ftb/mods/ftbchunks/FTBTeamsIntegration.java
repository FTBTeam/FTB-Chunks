package dev.ftb.mods.ftbchunks;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public class FTBTeamsIntegration {
	public static boolean isTeamMember(GameProfile profile, GameProfile profile2) {
		/*
		Optional<Team> team1 = FTBTeamsAPI.getManager().getPlayerTeam(profile);

		if (team1.isPresent()) {
			Optional<Team> team2 = FTBTeamsAPI.getManager().getPlayerTeam(profile2);
			return team2.isPresent() && team1.get().equals(team2.get());
		}
		*/

		return false;
	}

	@Nullable
	public static Component getTeamName(ClaimedChunkPlayerData data) {
		// return FTBTeamsAPI.getManager().getPlayerTeam(data.getUuid()).map(team -> team.getName().copy()).orElse(null);
		return new TextComponent("Test");
	}

	public static int getTeamColor(ClaimedChunkPlayerData data) {
		// return FTBTeamsAPI.getManager().getPlayerTeam(data.getUuid()).map(team -> team.getProperty(TeamImpl.COLOR)).orElse(0);
		return 0xFFFFFF;
	}
}