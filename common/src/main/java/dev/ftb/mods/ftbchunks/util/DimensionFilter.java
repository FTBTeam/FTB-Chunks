package dev.ftb.mods.ftbchunks.util;

import dev.ftb.mods.ftbchunks.config.FTBChunksWorldConfig;
import dev.ftb.mods.ftblibrary.util.Lazy;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

public class DimensionFilter {
    private static final Lazy<WildcardedRLMatcher> DIMENSION_MATCHER_B
            = Lazy.of(() -> new WildcardedRLMatcher(FTBChunksWorldConfig.CLAIM_DIMENSION_BLACKLIST.get()));
    private static final Lazy<WildcardedRLMatcher> DIMENSION_MATCHER_W
            = Lazy.of(() -> new WildcardedRLMatcher(FTBChunksWorldConfig.CLAIM_DIMENSION_WHITELIST.get()));
    private static final Lazy<WildcardedRLMatcher> NO_WILDERNESS
            = Lazy.of(() -> new WildcardedRLMatcher(FTBChunksWorldConfig.NO_WILDERNESS_DIMENSIONS.get()));

    public static boolean isDimensionOK(ResourceKey<Level> levelKey) {
        Identifier name = levelKey.identifier();
        return !getDimensionBlacklist().test(name) && (getDimensionWhitelist().isEmpty() || getDimensionWhitelist().test(name));
    }

    public static boolean isNoWildernessDimension(ResourceKey<Level> levelKey) {
        return getNoWildernessList().test(levelKey.identifier());
    }

    private static WildcardedRLMatcher getDimensionWhitelist() {
        return DIMENSION_MATCHER_W.get();
    }

    private static WildcardedRLMatcher getDimensionBlacklist() {
        return DIMENSION_MATCHER_B.get();
    }

    private static WildcardedRLMatcher getNoWildernessList() {
        return NO_WILDERNESS.get();
    }

    public static void clearMatcherCaches() {
        DIMENSION_MATCHER_B.invalidate();
        DIMENSION_MATCHER_W.invalidate();
        NO_WILDERNESS.invalidate();
    }

    private static class WildcardedRLMatcher implements Predicate<Identifier> {
        private final Set<String> namespaces = new ObjectOpenHashSet<>();
        private final Set<Identifier> reslocs = new ObjectOpenHashSet<>();

        public WildcardedRLMatcher(Collection<String> toMatch) {
            Identifier location;
            for (String s : toMatch) {
                if (s.endsWith(":*")) {
                    namespaces.add(s.split(":")[0]);
                } else if ((location = Identifier.tryParse(s)) != null) {
                    reslocs.add(location);
                }
            }
        }

        public boolean isEmpty() {
            return reslocs.isEmpty() && namespaces.isEmpty();
        }

        @Override
        public boolean test(Identifier Identifier) {
            return reslocs.contains(Identifier) || namespaces.contains(Identifier.getNamespace());
        }
    }
}
