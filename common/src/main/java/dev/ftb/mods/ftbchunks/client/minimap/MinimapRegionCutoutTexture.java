package dev.ftb.mods.ftbchunks.client.minimap;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftbchunks.client.map.MapRegionTexture;
import dev.ftb.mods.ftbchunks.config.FTBChunksClientConfig;
import dev.ftb.mods.ftblibrary.math.XZ;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
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

        texture = new DynamicTexture(ID::toString, image) {
            @Override
            public void upload() {
                FilterMode filter = FTBChunksClientConfig.shouldBlurTexture(FTBChunksClientConfig.MINIMAP_ZOOM.get()) ?
                        FilterMode.LINEAR :
                        FilterMode.NEAREST;
                sampler = RenderSystem.getSamplerCache().getSampler(
                        AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, filter, filter, false
                );
                super.upload();
            }
        };
        Minecraft.getInstance().getTextureManager().register(ID, texture);
    }

    public void update(ResourceKey<Level> key, XZ chunkPos) {
        if (MapDimension.getCurrent().isEmpty()) {
            return;
        }
        MapDimension dim = MapDimension.getCurrent().get();
        if (dim.dimension != key) {
            return;
        }

        var size = FTBChunks.MINIMAP_SIZE;

        image.fillRect(0, 0, size, size, 0);

        // The TILES x TILES window of chunks can only ever straddle at most 2 regions on each axis
        // (regions are 32x32 chunks, and TILES < 32), so split each axis into contiguous runs of
        // chunks that share the same region and copy one rectangular block per region touched
        // (at most 4 copies total) instead of one per chunk.
        int[] regionSplitX = regionSplits(chunkPos.x());
        int[] regionSplitZ = regionSplits(chunkPos.z());

        for (int ix = 0; ix < regionSplitX.length - 1; ix++) {
            int x1 = regionSplitX[ix];
            int x2 = regionSplitX[ix + 1];
            int ox = chunkPos.x() + x1 - FTBChunks.TILE_OFFSET;

            for (int iz = 0; iz < regionSplitZ.length - 1; iz++) {
                int z1 = regionSplitZ[iz];
                int z2 = regionSplitZ[iz + 1];
                int oz = chunkPos.z() + z1 - FTBChunks.TILE_OFFSET;

                MapRegion region = dim.getRegion(XZ.regionFromChunk(ox, oz));
                MapRegionTexture regionTexture = region.regionTexture();
                DynamicTexture dynamicTexture = regionTexture.bakedTexture();

                if (dynamicTexture == null) {
                    // Trigger baking if not already in progress
                    regionTexture.requestBake();
                    continue;
                }

                NativeImage regionImage = dynamicTexture.getPixels();

                int imgSize = regionImage.getWidth();
                int chunksPerRegion = imgSize / 16;

                int srcX = (ox & (chunksPerRegion - 1)) * 16;
                int srcZ = (oz & (chunksPerRegion - 1)) * 16;

                int dstX = x1 * 16;
                int dstZ = z1 * 16;

                regionImage.copyRect(image, srcX, srcZ, dstX, dstZ, (x2 - x1) * 16, (z2 - z1) * 16, false, false);
            }
        }

        texture.upload();
    }

    // Splits the [0, FTBChunks.TILES) tile index range on the given axis into contiguous runs of
    // chunks that fall within the same region, given the axis' centre chunk coordinate. Returned as
    // boundary indices, e.g. {0, 5, 15} means tiles [0,5) are in one region and [5,15) are in the next.
    private static int[] regionSplits(int centreChunk) {
        int firstRegion = (centreChunk - FTBChunks.TILE_OFFSET) >> 5;
        for (int m = 1; m < FTBChunks.TILES; m++) {
            if (((centreChunk + m - FTBChunks.TILE_OFFSET) >> 5) != firstRegion) {
                return new int[]{0, m, FTBChunks.TILES};
            }
        }
        return new int[]{0, FTBChunks.TILES};
    }
}
