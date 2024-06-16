package dev.ftb.mods.ftbchunks.util;

import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

public class DimensionFilter {
    private static WildcardedRLMatcher dimensionMatcherB = null;
    private static WildcardedRLMatcher dimensionMatcherW = null;
    private static WildcardedRLMatcher noWilderness = null;

    public static boolean isDimensionOK(ResourceKey<Level> levelKey) {
        ResourceLocation name = levelKey.location();
        return !getDimensionBlacklist().test(name) && (getDimensionWhitelist().isEmpty() || getDimensionWhitelist().test(name));
    }

    public static boolean isNoWildernessDimension(ResourceKey<Level> levelKey) {
        return getNoWildernessList().test(levelKey.location());
    }

    private static WildcardedRLMatcher getDimensionWhitelist() {
        if (dimensionMatcherW == null) {
            dimensionMatcherW = new WildcardedRLMatcher(FTBChunksWorldConfig.CLAIM_DIMENSION_WHITELIST.get());
        }
        return dimensionMatcherW;
    }

    private static WildcardedRLMatcher getDimensionBlacklist() {
        if (dimensionMatcherB == null) {
            dimensionMatcherB = new WildcardedRLMatcher(FTBChunksWorldConfig.CLAIM_DIMENSION_BLACKLIST.get());
        }
        return dimensionMatcherB;
    }

    private static WildcardedRLMatcher getNoWildernessList() {
        if (noWilderness == null) {
            noWilderness = new WildcardedRLMatcher(FTBChunksWorldConfig.NO_WILDERNESS_DIMENSIONS.get());
        }
        return noWilderness;
    }

    public static void clearMatcherCaches() {
        dimensionMatcherB = null;
        dimensionMatcherW = null;
        noWilderness = null;
    }

    private static class WildcardedRLMatcher implements Predicate<ResourceLocation> {
        private final Set<String> namespaces = new ObjectOpenHashSet<>();
        private final Set<ResourceLocation> reslocs = new ObjectOpenHashSet<>();

        public WildcardedRLMatcher(Collection<String> toMatch) {
            ResourceLocation location;
            for (String s : toMatch) {
                if (s.endsWith(":*")) {
                    namespaces.add(s.split(":")[0]);
                } else if ((location = ResourceLocation.tryParse(s)) != null) {
                    reslocs.add(location);
                }
            }
        }

        public boolean isEmpty() {
            return reslocs.isEmpty() && namespaces.isEmpty();
        }

        @Override
        public boolean test(ResourceLocation resourceLocation) {
            return reslocs.contains(resourceLocation) || namespaces.contains(resourceLocation.getNamespace());
        }
    }
}
