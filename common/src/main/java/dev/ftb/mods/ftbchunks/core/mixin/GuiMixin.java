package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "extractEffects", at = @At("HEAD"))
    public void onRenderEffectsEnter(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo info) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((int) FTBChunksClient.INSTANCE.getMinimapRenderer().getVanillaEffectsOffsetX(), 0);
    }

    @Inject(method = "extractEffects", at = @At("RETURN"))
    public void onRenderEffectsReturn(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker, CallbackInfo info) {
        guiGraphics.pose().popMatrix();
    }
}
