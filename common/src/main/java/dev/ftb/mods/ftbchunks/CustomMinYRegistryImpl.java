package dev.ftb.mods.ftbchunks;

import dev.ftb.mods.ftbchunks.api.LevelMinYCalculator;
import dev.ftb.mods.ftbchunks.api.event.CustomMinYEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CopyOnWriteArrayList;

public class CustomMinYRegistryImpl implements CustomMinYEvent.CustomMinYRegistry {
    private static final CustomMinYRegistryImpl serverInstance = new CustomMinYRegistryImpl();
    private static final CustomMinYRegistryImpl clientInstance = new CustomMinYRegistryImpl();

    private final List<LevelMinYCalculator> calculators = new CopyOnWriteArrayList<>();  // needs to be threadsafe

    public static CustomMinYRegistryImpl getInstance(boolean isClientSide) {
        return isClientSide ? clientInstance : serverInstance;
    }

    public void register(LevelMinYCalculator calculator) {
        calculators.add(calculator);
    }

    public int getMinYAt(Level level, BlockPos pos) {
        for (var calc : calculators) {
            OptionalInt h = calc.getLevelMinY(level, pos);
            if (h.isPresent()) {
                return h.getAsInt();
            }
        }
        return level.getMinBuildHeight();
    }
}
