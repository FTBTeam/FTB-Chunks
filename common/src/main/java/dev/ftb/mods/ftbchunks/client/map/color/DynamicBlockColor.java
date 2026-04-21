package dev.ftb.mods.ftbchunks.client.map.color;

import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.platform.Platform;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

public enum DynamicBlockColor implements BlockColor {
    INSTANCE;

    @Override
    public Color4I getBlockColor(BlockAndTintGetter blockAndTintGetter, BlockPos pos) {
        BlockState state = blockAndTintGetter.getBlockState(pos);
        // state shouldn't be null here, but we can run off-thread so let's be paranoid
        //noinspection ConstantValue
        return state == null ?
                Color4I.BLACK :
                Color4I.rgb(Platform.get().misc().getMapColor(state, blockAndTintGetter, pos, MapColor.NONE).col);
    }

    @Override
    public boolean isIgnored(Level level, BlockPos pos, BlockState state) {
        return Platform.get().misc().getMapColor(state, level, pos, MapColor.NONE) == MapColor.NONE;
    }
}
