package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.core.LevelAccessFTBC;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin implements LevelAccessFTBC {
	@Override
	@Accessor("level")
	public abstract Level getLevelFTBC();
}
