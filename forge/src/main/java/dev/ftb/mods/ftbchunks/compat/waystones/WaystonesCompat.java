package dev.ftb.mods.ftbchunks.compat.waystones;

import net.blay09.mods.waystones.api.IWaystone;
import net.blay09.mods.waystones.api.KnownWaystonesEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WaystonesCompat {

    private static List<IWaystone> WAYSTONES = Collections.emptyList();

    @SubscribeEvent
    public static void onWaystonesReceived(KnownWaystonesEvent event) {
        WAYSTONES = event.getWaystones();
    }

    public static Stream<IWaystone> getWaystones(ResourceKey<Level> dimension) {
        return WAYSTONES.stream().filter(w -> w.getDimension().equals(dimension));
    }

}
