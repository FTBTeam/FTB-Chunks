package dev.ftb.mods.ftbchunks.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.gui.map.ChunkScreen;
import dev.ftb.mods.ftblibrary.client.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class MapRegionTexture {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final MapRegion region;
    private final Identifier textureID;

    private volatile boolean baking = false;
    @Nullable
    private DynamicTexture texture;

    public MapRegionTexture(MapRegion region) {
        this.region = region;
        this.textureID = makeTextureId(region);
    }

    private static Identifier makeTextureId(MapRegion region) {
        String id = region.dimension.dimension.identifier().toString().replace(':', '_');
        return FTBChunksAPI.id(id + "/" + region.pos.x() + "_" + region.pos.z());
    }

    @Nullable
    public Identifier getTextureID() {
        if (!isBaking() && texture == null) {
            requestBake();
            return null;
        }

        return texture == null ? null : textureID;
    }

    public void requestBake() {
        if (isBaking()) {
            return;
        }

        baking = true;
        FTBChunksClient.MAP_EXECUTOR.execute(new RenderMapImageTask(this.region, image -> {
            baking = false;
            // Ensure that we upload on the main thread
            Minecraft.getInstance().execute(() -> upload(image));
        }));
    }

    private void upload(NativeImage image) {
        if (texture == null) {
            // First time - create texture
            texture = new DynamicTexture(textureID::toString, image);
            Minecraft.getInstance().getTextureManager().register(textureID, this.texture);
        } else {
            // Subsequent times - just update pixels and upload
            texture.setPixels(image);
            texture.upload();
        }
        if (ClientUtils.getCurrentGuiAs(ChunkScreen.class) != null) {
            // make sure displayed claims get updated promptly in the chunk mgmt screen
            FTBChunksClient.INSTANCE.getMinimapRenderer().requestTextureRefresh();
        }
    }

    public void close() {
        if (texture == null) {
            throw new IllegalStateException("Texture is null");
        }

        try {
            Minecraft.getInstance().getTextureManager().release(textureID);
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

    @Nullable
    public DynamicTexture bakedTexture() {
        return texture;
    }
}
