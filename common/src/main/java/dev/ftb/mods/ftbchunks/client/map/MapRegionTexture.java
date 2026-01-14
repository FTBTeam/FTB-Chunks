package dev.ftb.mods.ftbchunks.client.map;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;

public class MapRegionTexture {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final MapRegion region;
    private final Identifier identifier;

    private boolean baking = false;

    private DynamicTexture texture;

    public MapRegionTexture(MapRegion region) {
        this.region = region;
        this.identifier = FTBChunksAPI.id(region.pos.x() + "_" + region.pos.z());
    }

    public @Nullable Identifier getTexture() {
        if (!isBaking() && texture == null) {
            requestBake();
            return null;
        }

        return texture == null ? null : identifier;
    }

    public void update() {
        this.requestBake();
    }

    public void requestBake() {
        if (isBaking()) {
            return;
        }

        baking = true;
        FTBChunksClient.MAP_EXECUTOR.execute(new RenderMapImageTask(this.region, (image) -> {
            baking = false;
            // Ensure that we upload on the main thread
            Minecraft.getInstance().execute(() -> upload(image));
        }));
    }

    private void upload(NativeImage image) {
        this.texture = new DynamicTexture(identifier::toString, image);
        Minecraft.getInstance().getTextureManager().register(identifier, this.texture);
    }

    public void close() {
        if (texture == null) {
            throw new IllegalStateException("Texture is null");
        }

        try {
            Minecraft.getInstance().getTextureManager().release(identifier);
            texture.close();
            texture = null;
        } catch (Exception error) {
            LOGGER.error("Failed to release texture", error);
        }
    }

    public boolean isBaking() {
        return baking;
    }

    public boolean isOpen() {
        return texture != null;
    }
}
