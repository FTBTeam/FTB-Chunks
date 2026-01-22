package dev.ftb.mods.ftbchunks.neoforge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.minimap.MinimapPIPRenderer;
import dev.ftb.mods.ftbchunks.client.minimap.MinimapRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

@Mod(value = FTBChunks.MOD_ID, dist = Dist.CLIENT)
public class FTBChunksNeoForgeClient {
    public FTBChunksNeoForgeClient(IEventBus modBus) {
        modBus.addListener(this::registerPIPRenderers);
    }

    private void registerPIPRenderers(RegisterPictureInPictureRenderersEvent event) {
        event.register(MinimapRenderState.class, MinimapPIPRenderer::new);
    }
}
