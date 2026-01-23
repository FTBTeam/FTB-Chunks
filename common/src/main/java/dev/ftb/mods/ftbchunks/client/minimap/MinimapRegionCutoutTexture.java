package dev.ftb.mods.ftbchunks.client.minimap;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftblibrary.math.XZ;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class MinimapRegionCutoutTexture {
    public static final Identifier ID = FTBChunksAPI.id("minimap_region_cutout_texture");
    private final NativeImage image;

    private final DynamicTexture texture;

    public MinimapRegionCutoutTexture() {
        var size = FTBChunks.MINIMAP_SIZE;

        // Reserve the texture.
        image = new NativeImage(NativeImage.Format.RGBA, size, size, true);
        image.fillRect(0, 0, size, size, 0);

        texture = new DynamicTexture(ID::toString, image);
        Minecraft.getInstance().getTextureManager().register(ID, texture);
    }

    public void update(ResourceKey<Level> key, int chunkX, int chunkZ) {
        if (MapDimension.getCurrent().isEmpty()) {
            return;
        }
        MapDimension dim = MapDimension.getCurrent().get();
        if (dim.dimension != key) {
            return;
        }

        var size = FTBChunks.MINIMAP_SIZE;

        image.fillRect(0, 0, size, size, 0);

        // Time to update.
        for (int mz = 0; mz < FTBChunks.TILES; mz++) {
            for (int mx = 0; mx < FTBChunks.TILES; mx++) {
                int ox = chunkX + mx - FTBChunks.TILE_OFFSET;
                int oz = chunkZ + mz - FTBChunks.TILE_OFFSET;

                MapRegion region = dim.getRegion(XZ.regionFromChunk(ox, oz));
                DynamicTexture dynamicTexture = region.regionTexture().bakedTexture();
                if (dynamicTexture == null) {
                    continue;
                }

                NativeImage regionImage = dynamicTexture.getPixels();
                if (regionImage == null) {
                    continue;
                }

                int imgSize = regionImage.getWidth();
                int chunksPerRegion = imgSize / 16;

                int srcX = (ox & (chunksPerRegion - 1)) * 16;
                int srcZ = (oz & (chunksPerRegion - 1)) * 16;

                int dstX = mx * 16;
                int dstZ = mz * 16;

                regionImage.copyRect(image, srcX, srcZ, dstX, dstZ, 16, 16, false, false);
            }
        }

        texture.upload();
    }

    public DynamicTexture getTexture() {
        return texture;
    }

    public Identifier identifier() {
        return ID;
    }
}
