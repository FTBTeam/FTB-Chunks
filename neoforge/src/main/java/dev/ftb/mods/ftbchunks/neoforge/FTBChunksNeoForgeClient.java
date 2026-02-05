package dev.ftb.mods.ftbchunks.neoforge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
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
    private final FTBChunksClient client;

    public FTBChunksNeoForgeClient(IEventBus modBus) {
        client = FTBChunksClient.INSTANCE.init();

        NeoForge.EVENT_BUS.addListener(this::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(this::onFrameGraphSetup);
    }

    private void onRenderLevelStage(RenderLevelStageEvent.AfterParticles event) {
        client.getInWorldIconRenderer().renderLevelStage(
                event.getModelViewMatrix(),
                event.getLevelRenderState().cameraRenderState.pos,
                Minecraft.getInstance().getDeltaTracker()
        );
    }

    private void onFrameGraphSetup(FrameGraphSetupEvent event) {
        client.getInWorldIconRenderer().copyProjectionMatrix(event.getProjectionMatrix());
    }
}
