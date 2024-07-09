package dev.ftb.mods.ftbchunks.api;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class FTBChunksTags {
    public static class Blocks {
        public static final TagKey<Block> EDIT_WHITELIST_TAG
                = TagKey.create(Registries.BLOCK, FTBChunksAPI.rl("edit_whitelist"));
        public static final TagKey<Block> INTERACT_WHITELIST_TAG
                = TagKey.create(Registries.BLOCK, FTBChunksAPI.rl("interact_whitelist"));
    }

    public static class Items {
        public static final TagKey<Item> RIGHT_CLICK_BLACKLIST_TAG
                = TagKey.create(Registries.ITEM, FTBChunksAPI.rl("right_click_blacklist"));
        public static final TagKey<Item> RIGHT_CLICK_WHITELIST_TAG
                = TagKey.create(Registries.ITEM, FTBChunksAPI.rl("right_click_whitelist"));
    }

    public static class Entities {
        public static final TagKey<EntityType<?>> ENTITY_INTERACT_WHITELIST_TAG
                = TagKey.create(Registries.ENTITY_TYPE, FTBChunksAPI.rl("entity_interact_whitelist"));
        public static final TagKey<EntityType<?>> NONLIVING_ENTITY_ATTACK_WHITELIST_TAG
                = TagKey.create(Registries.ENTITY_TYPE, FTBChunksAPI.rl("nonliving_entity_attack_whitelist"));
        public static final TagKey<EntityType<?>> ENTITY_MOB_GRIEFING_BLACKLIST_TAG
                = TagKey.create(Registries.ENTITY_TYPE, FTBChunksAPI.rl("entity_mob_griefing_blacklist"));
    }
}
