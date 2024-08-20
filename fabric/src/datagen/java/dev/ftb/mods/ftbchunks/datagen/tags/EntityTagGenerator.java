package dev.ftb.mods.ftbchunks.datagen.tags;

import dev.ftb.mods.ftbchunks.api.FTBChunksTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.EntityType;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class EntityTagGenerator extends FabricTagProvider.EntityTypeTagProvider {

    public EntityTagGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        getOrCreateTagBuilder(FTBChunksTags.Entities.ENTITY_INTERACT_WHITELIST_TAG)
                // Corpse
                .addOptional(fromNamespaceAndPath("corpse", "corpse"));

        getOrCreateTagBuilder(FTBChunksTags.Entities.ENTITY_MOB_GRIEFING_BLACKLIST_TAG)
                // Minecraft
                .add(EntityType.CREEPER);
    }
}