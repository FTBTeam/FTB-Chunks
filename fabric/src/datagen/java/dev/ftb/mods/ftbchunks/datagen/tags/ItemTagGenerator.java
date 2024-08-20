package dev.ftb.mods.ftbchunks.datagen.tags;

import dev.ftb.mods.ftbchunks.api.FTBChunksTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ItemTagGenerator extends FabricTagProvider.ItemTagProvider {

    public ItemTagGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        getOrCreateTagBuilder(FTBChunksTags.Items.RIGHT_CLICK_BLACKLIST_TAG)
                .add(Items.BUCKET)
                .add(Items.WATER_BUCKET)
                .add(Items.LAVA_BUCKET)
                .add(Items.LEAD)
                .add(Items.NAME_TAG)
                .add(Items.SPLASH_POTION)
                .add(Items.LINGERING_POTION);

        getOrCreateTagBuilder(FTBChunksTags.Items.RIGHT_CLICK_WHITELIST_TAG)
                .add(Items.SHIELD)
                .add(Items.FIREWORK_ROCKET)
                .add(Items.MAP);

    }
}