package dev.ftb.mods.ftbchunks.datagen.tags;

import dev.ftb.mods.ftbchunks.api.FTBChunksTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;

public class BlockTagGenerator extends FabricTagProvider.BlockTagProvider {

        public BlockTagGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
                super(output, registriesFuture);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
                getOrCreateTagBuilder(FTBChunksTags.Blocks.EDIT_WHITELIST_TAG)
                        // Gravestone
                        .addOptional(fromNamespaceAndPath("gravestone", "gravestone"));

                getOrCreateTagBuilder(FTBChunksTags.Blocks.INTERACT_WHITELIST_TAG)
                        // Minecraft
                        .add(Blocks.CRAFTING_TABLE)
                        .add(Blocks.SMITHING_TABLE)
                        .add(Blocks.BELL)
                        // Tombstone
                        .addOptionalTag(fromNamespaceAndPath("tombstone", "graves"))
                        // Waystones
                        .addOptionalTag(fromNamespaceAndPath("waystones", "waystones"))
                        .addOptionalTag(fromNamespaceAndPath("waystones", "sharestones"))
                        // Waystones (Fabric)
                        .addOptionalTag(fromNamespaceAndPath("fwaystones", "waystones"));
        }

}