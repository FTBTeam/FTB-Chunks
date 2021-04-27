package dev.ftb.mods.ftbchunks;

import me.shedaniel.architectury.annotations.ExpectPlatform;
import net.minecraft.world.level.block.Block;

public class FTBCUtils {
	@ExpectPlatform
	public static boolean isRail(Block block) {
		throw new AssertionError();
	}

	public static String prettyTimeString(long seconds) {
		if (seconds <= 0L) {
			return "0 seconds";
		}

		StringBuilder builder = new StringBuilder();
		prettyTimeString(builder, seconds, true);
		return builder.toString();
	}

	private static void prettyTimeString(StringBuilder builder, long seconds, boolean addAnother) {
		if (seconds <= 0L) {
			return;
		} else if (!addAnother) {
			builder.append(" and ");
		}

		if (seconds < 60L) {
			builder.append(seconds);
			builder.append(seconds == 1L ? " second" : " seconds");
		} else if (seconds < 3600L) {
			builder.append(seconds / 60L);
			builder.append(seconds / 60L == 1L ? " minute" : " minutes");

			if (addAnother) {
				prettyTimeString(builder, seconds % 60L, false);
			}
		} else if (seconds < 86400L) {
			builder.append(seconds / 3600L);
			builder.append(seconds / 3600L == 1L ? " hour" : " hours");

			if (addAnother) {
				prettyTimeString(builder, seconds % 3600L, false);
			}
		} else {
			builder.append(seconds / 86400L);
			builder.append(seconds / 86400L == 1L ? " day" : " days");

			if (addAnother) {
				prettyTimeString(builder, seconds % 86400L, false);
			}
		}
	}
}
