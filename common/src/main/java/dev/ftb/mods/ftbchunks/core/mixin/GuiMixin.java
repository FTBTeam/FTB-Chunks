package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "renderEffects", at = @At("HEAD"))
    public void onRenderEffectsEnter(GuiGraphics guiGraphics, CallbackInfo info) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(FTBChunksClient.getVanillaEffectsOffsetX(), 0, 0);
    }

    @Inject(method = "renderEffects", at = @At("RETURN"))
    public void onRenderEffectsReturn(GuiGraphics guiGraphics, CallbackInfo info) {
        guiGraphics.pose().popPose();
    }
}
