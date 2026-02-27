package dev.ftb.mods.ftbchunks.fabric.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    public void renderLevel(GraphicsResourceAllocator arg,
                            DeltaTracker arg2,
                            boolean bl,
                            Camera arg3,
                            Matrix4f matrix4f,
                            Matrix4f matrix4f2,
                            Matrix4f matrix4f3,
                            GpuBufferSlice gpuBufferSlice,
                            Vector4f vector4f,
                            boolean bl2,
                            CallbackInfo ci) {
        FTBChunksClient.INSTANCE.getInWorldIconRenderer().copyProjectionMatrix(matrix4f2);
    }
}
