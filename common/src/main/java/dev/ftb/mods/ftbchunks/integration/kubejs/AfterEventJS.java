package dev.ftb.mods.ftbchunks.integration.kubejs;

import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.latvian.mods.kubejs.entity.EntityEventJS;
import dev.latvian.mods.kubejs.entity.EntityJS;
import dev.latvian.mods.kubejs.level.LevelJS;
import dev.latvian.mods.kubejs.player.ServerPlayerJS;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class AfterEventJS extends EntityEventJS {
	public final CommandSourceStack source;
	public final ClaimedChunk chunk;

	public AfterEventJS(CommandSourceStack s, ClaimedChunk c) {
		source = s;
		chunk = c;
	}

	@Override
	public LevelJS getLevel() {
		return levelOf(source.getLevel());
	}

	@Override
	@Nullable
	public EntityJS getEntity() {
		return source.getEntity() != null ? entityOf(source.getEntity()) : null;
	}

	@Nullable
	public ServerPlayerJS getPlayer() {
		return source.getEntity() instanceof ServerPlayer ? (ServerPlayerJS) getEntity() : null;
	}
}
