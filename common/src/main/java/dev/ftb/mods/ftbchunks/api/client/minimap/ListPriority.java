package dev.ftb.mods.ftbchunks.api.client.minimap;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * TODO: Javadoc
 */
public class ListPriority {
    public static final ResourceLocation DEFAULT_ORDER = FTBChunksAPI.rl("default");

    @Nullable private ResourceLocation before = null;
    @Nullable private ResourceLocation after = DEFAULT_ORDER;

    private ListPriority(@Nullable ResourceLocation before, @Nullable ResourceLocation after) {
        this.before = before;
        this.after = after;
    }

    private ListPriority() {}

    public static ListPriority defaultOrder() {
        return new ListPriority();
    }

    public static ListPriority after(ResourceLocation location) {
        return new ListPriority(null, location);
    }

    public static ListPriority before(ResourceLocation location) {
        return new ListPriority(location, null);
    }

    public boolean isDefault() {
        return this.before == null && this.after == DEFAULT_ORDER;
    }

    public @Nullable ResourceLocation getAfter() {
        return after;
    }

    public @Nullable ResourceLocation getBefore() {
        return before;
    }
}
