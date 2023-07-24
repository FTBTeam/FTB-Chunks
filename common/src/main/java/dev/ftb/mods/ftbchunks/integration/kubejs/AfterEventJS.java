package dev.ftb.mods.ftbchunks.integration.kubejs;

import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.latvian.mods.kubejs.entity.EntityEventJS;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class AfterEventJS extends EntityEventJS {
	public final CommandSourceStack source;
	public final ClaimedChunk chunk;

	public AfterEventJS(CommandSourceStack s, ClaimedChunk c) {
		source = s;
		chunk = c;
	}

	@Override
	public Level getLevel() {
		return source.getLevel();
	}

	@Override
	@Nullable
	public Entity getEntity() {
		return source.getEntity();
	}

	@Nullable
	public ServerPlayer getPlayer() {
        return source.getEntity() instanceof ServerPlayer sp ? sp : null;
    }

	public BlockPos getClaimPos() {
		return chunk.pos.getChunkPos().getWorldPosition();
	}
}
