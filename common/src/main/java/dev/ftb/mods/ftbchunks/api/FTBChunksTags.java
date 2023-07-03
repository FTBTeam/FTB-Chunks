package dev.ftb.mods.ftbchunks.api;

import dev.ftb.mods.ftbchunks.FTBChunks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class FTBChunksTags {
    public static class Blocks {
        public static final TagKey<Block> EDIT_WHITELIST_TAG
                = TagKey.create(Registries.BLOCK, new ResourceLocation(FTBChunks.MOD_ID, "edit_whitelist"));
        public static final TagKey<Block> INTERACT_WHITELIST_TAG
                = TagKey.create(Registries.BLOCK, new ResourceLocation(FTBChunks.MOD_ID, "interact_whitelist"));
    }

    public static class Items {
        public static final TagKey<Item> RIGHT_CLICK_BLACKLIST_TAG
                = TagKey.create(Registries.ITEM, new ResourceLocation(FTBChunks.MOD_ID, "right_click_blacklist"));
        public static final TagKey<Item> RIGHT_CLICK_WHITELIST_TAG
                = TagKey.create(Registries.ITEM, new ResourceLocation(FTBChunks.MOD_ID, "right_click_whitelist"));
    }

    public static class Entities {
        public static final TagKey<EntityType<?>> ENTITY_INTERACT_WHITELIST_TAG
                = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(FTBChunks.MOD_ID, "entity_interact_whitelist"));
        public static final TagKey<EntityType<?>> NONLIVING_ENTITY_ATTACK_WHITELIST_TAG
                = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(FTBChunks.MOD_ID, "nonliving_entity_attack_whitelist"));
    }
}
