package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.core.MouseHandlerFTBC;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin implements MouseHandlerFTBC {
	@Override
	@Accessor("mouseGrabbed")
	public abstract void setMouseGrabbedFTBC(boolean b);
}
