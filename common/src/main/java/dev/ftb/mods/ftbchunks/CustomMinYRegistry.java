package dev.ftb.mods.ftbchunks;

import dev.ftb.mods.ftbchunks.api.LevelMinYCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CopyOnWriteArrayList;

public class CustomMinYRegistry {
    private static final CustomMinYRegistry serverInstance = new CustomMinYRegistry();
    private static final CustomMinYRegistry clientInstance = new CustomMinYRegistry();

    private final List<LevelMinYCalculator> calculators = new CopyOnWriteArrayList<>();  // needs to be threadsafe

    public static CustomMinYRegistry getInstance(boolean isClientSide) {
        return isClientSide ? clientInstance : serverInstance;
    }

    public void register(List<LevelMinYCalculator> calculators) {
        this.calculators.addAll(calculators);
    }

    public int getMinYAt(Level level, BlockPos pos) {
        for (var calc : calculators) {
            OptionalInt h = calc.getLevelMinY(level, pos);
            if (h.isPresent()) {
                return h.getAsInt();
            }
        }
        return level.getMinY();
    }
}
