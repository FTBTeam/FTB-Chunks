package dev.ftb.mods.ftbchunks.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class LevelDataDirectory {

    public static String getDirectoryFromDimensionKey(ResourceKey<Level> levelKey, String subDirectory) {
        if (Level.OVERWORLD.equals(levelKey)) {
            return FTBChunksAPI.getManager().levelDataDirectory.toString() + "/"+ subDirectory+ "/";
        } else if (Level.NETHER.equals(levelKey)) {
            return FTBChunksAPI.getManager().levelDataDirectory.toString() + "/DIM-1/"+ subDirectory+ "/";
        } else if (Level.END.equals(levelKey)) {
            return FTBChunksAPI.getManager().levelDataDirectory.toString() + "/DIM1/"+ subDirectory+ "/";
        } else {
            return FTBChunksAPI.getManager().levelDataDirectory.toString() + "/dimensions/"
                + levelKey.location().getNamespace() + "/" + levelKey.location().getPath() + "/"+ subDirectory+ "/";
        }
    }
}