package dev.ftb.mods.ftbchunks.api;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbteams.api.property.BooleanProperty;
import dev.ftb.mods.ftbteams.api.property.PrivacyMode;
import dev.ftb.mods.ftbteams.api.property.PrivacyProperty;
import dev.ftb.mods.ftbteams.api.property.StringListProperty;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

public class FTBChunksProperties {
    public static final BooleanProperty ALLOW_ALL_FAKE_PLAYERS = new BooleanProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_fake_players"), false);
    public static final StringListProperty ALLOW_NAMED_FAKE_PLAYERS = new StringListProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_named_fake_players"), new ArrayList<>());
    public static final BooleanProperty ALLOW_FAKE_PLAYERS_BY_ID = new BooleanProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_fake_players_by_id"), true);
    // forge
    public static final PrivacyProperty BLOCK_EDIT_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "block_edit_mode"), PrivacyMode.ALLIES);
    public static final PrivacyProperty BLOCK_INTERACT_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "block_interact_mode"), PrivacyMode.ALLIES);
    // fabric
    public static final PrivacyProperty BLOCK_EDIT_AND_INTERACT_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "block_edit_and_interact_mode"), PrivacyMode.ALLIES);
    public static final PrivacyProperty ENTITY_INTERACT_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "entity_interact_mode"), PrivacyMode.ALLIES);
    public static final PrivacyProperty NONLIVING_ENTITY_ATTACK_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "nonliving_entity_attack_mode"), PrivacyMode.ALLIES);
    public static final BooleanProperty ALLOW_EXPLOSIONS = new BooleanProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_explosions"), false);
    public static final BooleanProperty ALLOW_MOB_GRIEFING = new BooleanProperty(new ResourceLocation(FTBChunks.MOD_ID, "allow_mob_griefing"), false);
    public static final PrivacyProperty CLAIM_VISIBILITY = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "claim_visibility"), PrivacyMode.PUBLIC);
    public static final PrivacyProperty LOCATION_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "location_mode"), PrivacyMode.ALLIES);

    //	public static final PrivacyProperty MINIMAP_MODE = new PrivacyProperty(new ResourceLocation(FTBChunks.MOD_ID, "minimap_mode"), PrivacyMode.ALLIES);
}
