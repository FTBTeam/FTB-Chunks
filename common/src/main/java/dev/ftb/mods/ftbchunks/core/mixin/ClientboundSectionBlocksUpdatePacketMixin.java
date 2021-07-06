package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.core.ClientboundSectionBlocksUpdatePacketFTBC;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSectionBlocksUpdatePacket.class)
public abstract class ClientboundSectionBlocksUpdatePacketMixin implements ClientboundSectionBlocksUpdatePacketFTBC {
	@Override
	@Accessor("sectionPos")
	public abstract SectionPos getSectionPosFTBC();

	@Override
	@Accessor("positions")
	public abstract short[] getPositionsFTBC();

	@Override
	@Accessor("states")
	public abstract BlockState[] getStatesFTBC();
}
