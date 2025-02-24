package dev.ftb.mods.ftbchunks.api;

import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbteams.api.property.BooleanProperty;
import dev.ftb.mods.ftbteams.api.property.PrivacyProperty;
import dev.ftb.mods.ftbteams.api.property.StringListProperty;

import java.util.ArrayList;

/**
 * These FTB Teams properties are added to teams by FTB Chunks, and appear in the team properties editor GUI.
 */
public class FTBChunksProperties {
    public static final BooleanProperty ALLOW_ALL_FAKE_PLAYERS
            = new BooleanProperty(FTBChunksAPI.rl("allow_fake_players"), FTBChunksWorldConfig.DEF_ALLOW_FAKE_PLAYERS::get);
    public static final StringListProperty ALLOW_NAMED_FAKE_PLAYERS
            = new StringListProperty(FTBChunksAPI.rl("allow_named_fake_players"), () -> new ArrayList<>(FTBChunksWorldConfig.DEF_ALLOW_NAMED_FAKE_PLAYERS.get()));
    public static final BooleanProperty ALLOW_FAKE_PLAYERS_BY_ID
            = new BooleanProperty(FTBChunksAPI.rl("allow_fake_players_by_id"), FTBChunksWorldConfig.DEF_ALLOW_FAKE_PLAYER_IDS::get);
    public static final PrivacyProperty ENTITY_INTERACT_MODE
            = new PrivacyProperty(FTBChunksAPI.rl("entity_interact_mode"), FTBChunksWorldConfig.DEF_ENTITY_INTERACT::get);
    public static final PrivacyProperty NONLIVING_ENTITY_ATTACK_MODE
            = new PrivacyProperty(FTBChunksAPI.rl("nonliving_entity_attack_mode"), FTBChunksWorldConfig.DEF_NONLIVING_ENTITY_ATTACK::get);
    public static final BooleanProperty ALLOW_EXPLOSIONS
            = new BooleanProperty(FTBChunksAPI.rl("allow_explosions"), FTBChunksWorldConfig.DEF_ALLOW_EXPLOSIONS::get);
    public static final BooleanProperty ALLOW_MOB_GRIEFING
            = new BooleanProperty(FTBChunksAPI.rl("allow_mob_griefing"), FTBChunksWorldConfig.DEF_MOB_GRIEFING::get);
    public static final PrivacyProperty CLAIM_VISIBILITY
            = new PrivacyProperty(FTBChunksAPI.rl("claim_visibility"), FTBChunksWorldConfig.DEF_CLAIM_VISIBILITY::get);
    public static final PrivacyProperty LOCATION_MODE
            = new PrivacyProperty(FTBChunksAPI.rl("location_mode"), FTBChunksWorldConfig.DEF_PLAYER_VISIBILITY::get);
    public static final BooleanProperty ALLOW_PVP
            = new BooleanProperty(FTBChunksAPI.rl("allow_pvp"), FTBChunksWorldConfig.DEF_PVP::get);

    // FTB Chunks on Forge adds two separate block edit & interact properties
    public static final PrivacyProperty BLOCK_EDIT_MODE
            = new PrivacyProperty(FTBChunksAPI.rl("block_edit_mode"), FTBChunksWorldConfig.DEF_BLOCK_EDIT::get);
    public static final PrivacyProperty BLOCK_INTERACT_MODE
            = new PrivacyProperty(FTBChunksAPI.rl("block_interact_mode"), FTBChunksWorldConfig.DEF_BLOCK_INTERACT::get);

    // FTB Chunks on Fabric adds a combined block edit & interact property
    public static final PrivacyProperty BLOCK_EDIT_AND_INTERACT_MODE
            = new PrivacyProperty(FTBChunksAPI.rl("block_edit_and_interact_mode"), FTBChunksWorldConfig.DEF_BLOCK_EDIT_INTERACT::get);
}
