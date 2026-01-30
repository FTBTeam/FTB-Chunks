package dev.ftb.mods.ftbchunks.neoforge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.minimap.MinimapPIPRenderer;
import dev.ftb.mods.ftbchunks.client.minimap.MinimapPIPRenderState;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.FrameGraphSetupEvent;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = FTBChunks.MOD_ID, dist = Dist.CLIENT)
public class FTBChunksNeoForgeClient {
    public FTBChunksNeoForgeClient(IEventBus modBus) {
        modBus.addListener(this::registerPIPRenderers);

        NeoForge.EVENT_BUS.addListener(this::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(this::onFrameGraphSetup);
    }

    private void registerPIPRenderers(RegisterPictureInPictureRenderersEvent event) {
        event.register(MinimapPIPRenderState.class, MinimapPIPRenderer::new);
    }

    private void onRenderLevelStage(RenderLevelStageEvent.AfterParticles event) {
        FTBChunksClient.INSTANCE.renderLevelStage(
                event.getModelViewMatrix(),
                event.getLevelRenderState().cameraRenderState.pos,
                Minecraft.getInstance().getDeltaTracker()
        );
    }

    private void onFrameGraphSetup(FrameGraphSetupEvent event) {
        FTBChunksClient.INSTANCE.copyProjectionMatrix(event.getProjectionMatrix());
    }
}
