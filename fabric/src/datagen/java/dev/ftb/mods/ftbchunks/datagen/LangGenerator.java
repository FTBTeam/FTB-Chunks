package dev.ftb.mods.ftbchunks.datagen;

import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.api.FTBChunksTags;
import dev.ftb.mods.ftbchunks.client.FTBChunksClientConfig;
import dev.ftb.mods.ftbchunks.client.MinimapBlurMode;
import dev.ftb.mods.ftbchunks.client.MinimapPosition;
import dev.ftb.mods.ftbchunks.client.map.BiomeBlendMode;
import dev.ftb.mods.ftbchunks.client.map.MapMode;
import dev.ftb.mods.ftbchunks.client.minimap.components.BiomeComponent;
import dev.ftb.mods.ftbchunks.client.minimap.components.DebugComponent;
import dev.ftb.mods.ftbchunks.client.minimap.components.FPSComponent;
import dev.ftb.mods.ftbchunks.client.minimap.components.GameTimeComponent;
import dev.ftb.mods.ftbchunks.client.minimap.components.PlayerPosInfoComponent;
import dev.ftb.mods.ftbchunks.client.minimap.components.RealTimeComponent;
import dev.ftb.mods.ftbchunks.client.minimap.components.ZoneInfoComponent;
import dev.ftb.mods.ftbchunks.data.AllyMode;
import dev.ftb.mods.ftbchunks.data.ForceLoadMode;
import dev.ftb.mods.ftbchunks.data.PartyLimitMode;
import dev.ftb.mods.ftbchunks.data.PvPMode;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.snbt.config.BaseValue;
import dev.ftb.mods.ftbteams.api.property.TeamProperty;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class LangGenerator extends FabricLanguageProvider {

    protected LangGenerator(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder builder) {
        builder.add("ftbchunks", "FTB Chunks");

        builder.add("ftbchunks.zoom_warning", "Max zoom-out limited due to memory constraints");

        // Client Configs
        addConfig(builder, FTBChunksClientConfig.APPEARANCE, "Appearance");
        addConfig(builder, FTBChunksClientConfig.NOISE, "Noise");
        addConfig(builder, FTBChunksClientConfig.SHADOWS, "Shadows");
        addConfig(builder, FTBChunksClientConfig.CHUNK_GRID, "Chunk grid");
        addConfig(builder, FTBChunksClientConfig.REDUCED_COLOR_PALETTE, "Reduced color palette");
        addConfig(builder, FTBChunksClientConfig.SATURATION, "Saturation");
        addConfig(builder, FTBChunksClientConfig.CLAIMED_CHUNKS_ON_MAP, "Show claimed chunks on map");
        addConfig(builder, FTBChunksClientConfig.OWN_CLAIMED_CHUNKS_ON_MAP, "Show your own claimed chunks on map");
        addConfig(builder, FTBChunksClientConfig.MAP_MODE, "Map mode");
        addConfig(builder, FTBChunksClientConfig.BIOME_BLEND, "Biome blend");
        addConfig(builder, FTBChunksClientConfig.WATER_HEIGHT_FACTOR, "Water height factor");
        addConfig(builder, FTBChunksClientConfig.ONLY_SURFACE_ENTITIES, "Only surface entities");

        addConfig(builder, FTBChunksClientConfig.ADVANCED, "Advanced Settings");
        addConfig(builder, FTBChunksClientConfig.DEBUG_INFO, "Debug info");

        addConfig(builder, FTBChunksClientConfig.WAYPOINTS, "Waypoints");
        addConfig(builder, FTBChunksClientConfig.IN_WORLD_WAYPOINTS, "Show waypoints in world");
        addConfig(builder, FTBChunksClientConfig.DEATH_WAYPOINTS, "Create death waypoints on death");
        addConfig(builder, FTBChunksClientConfig.IN_WORLD_WAYPOINTS, "Show waypoints in world");
        addConfig(builder, FTBChunksClientConfig.DEATH_WAYPOINTS, "Create death waypoints on death");
        addConfig(builder, FTBChunksClientConfig.DEATH_WAYPOINT_AUTOREMOVE_DISTANCE, "Auto-remove deathpoint distance", "If > 0, the closest deathpoint will be auto-removed if it is closer than this distance from you");
        addConfig(builder, FTBChunksClientConfig.WAYPOINT_BEACON_FADE_DISTANCE, "Min beacon fade distance");
        addConfig(builder, FTBChunksClientConfig.WAYPOINT_DOT_FADE_DISTANCE, "Min dot fade distance");
        addConfig(builder, FTBChunksClientConfig.WAYPOINT_MAX_DISTANCE, "Max waypoint draw distance");
        addConfig(builder, FTBChunksClientConfig.WAYPOINT_FOCUS_DISTANCE, "Focused waypoint distance", "How close does the player crosshair need to be to a waypoint to display its name?");
        addConfig(builder, FTBChunksClientConfig.WAYPOINT_FOCUS_SCALE, "Focused waypoint scaling", "How big do focused waypoints grow?");

        addConfig(builder, FTBChunksClientConfig.MEMORY, "Memory Usage");
        addConfig(builder, FTBChunksClientConfig.REGION_RELEASE_TIME, "Idle region release timeout", "Timeout in seconds to release data for 512x512 regions which haven't been accessed recently\nSmaller values mean less memory usage, but more disk access as released regions are reloaded\nSet to 0 to disable releasing of region data.");
        addConfig(builder, FTBChunksClientConfig.AUTORELEASE_ON_MAP_CLOSE, "Autorelease regions on map close", "When the large map is closed, auto-release region data down to this number.\nSmaller values mean less memory usage, but more disk access as released regions are reloaded\nSet to 0 to disable releasing of region data.");
        addConfig(builder, FTBChunksClientConfig.MAX_ZOOM_CONSTRAINT, "Constrain map zoom-out", "When true, max map zoom-out is limited by the number of explored regions and amount of available memory.\nIf this is bothersome, set this to false.");

        addConfig(builder, FTBChunksClientConfig.MINIMAP, "Minimap");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_ENABLED, "Enabled");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_POSITION, "Position");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_SCALE, "Scale");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_LOCKED_NORTH, "Locked north");
        addConfig(builder, FTBChunksClientConfig.SHOW_PLAYER_WHEN_UNLOCKED, "Show player when not locked north");
        addConfig(builder, FTBChunksClientConfig.WAYPOINTS, "Waypoints");
        addConfig(builder, FTBChunksClientConfig.ONLY_SURFACE_ENTITIES, "Waypoints");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_ENTITIES, "Entities");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_ENTITY_HEADS, "Entity heads");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_PLAYER_HEADS, "Player heads");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_LARGE_ENTITIES, "Large entities");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_BLUR_MODE, "Blur mode");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_ZOOM, "Zoom");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_COMPASS, "Compass");

        addConfig(builder, FTBChunksClientConfig.MINIMAP_POSITION_OFFSET_CONDITION, "Position Offset condition");
        addConfig(builder, FTBChunksClientConfig.SQUARE_MINIMAP, "Square minimap");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_VISIBILITY, "Visibility");
        addConfig(builder, FTBChunksClientConfig.WAYPOINTS, "Waypoints");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_FONT_SCALE, "Font Scaling", "Recommended to keep this to a multiple of 0.5");
        addConfig(builder, FTBChunksClientConfig.MINIMAP_PROPORTIONAL, "Proportional sizing", "When true, minimap size is proportional to 10% of the screen width (modifiable by the Scale setting)\nWhen false, it is a fixed size regardless of screen resolution");
        builder.add("sidebar_button.ftbchunks.chunks", "FTB Chunks: Map");
        builder.add("sidebar_button.ftbchunks.claim_chunks", "FTB Chunks: Claim Manager");

        builder.add("key.categories.ftbchunks", "FTB Chunks");
        builder.add("key.ftbchunks.map", "Open Map");
        builder.add("key.ftbchunks.claim_manager", "Open Claim Manager");
        builder.add("key.ftbchunks.minimap.zoomIn", "Zoom In Minimap");
        builder.add("key.ftbchunks.minimap.zoomOut", "Zoom Out Minimap");
        builder.add("key.ftbchunks.add_waypoint", "Quick Add Waypoint");
        builder.add("key.ftbchunks.waypoint_manager", "Waypoint Manager");
        builder.add("key.ftbchunks.toggle_minimap", "Toggle Minimap");

        builder.add("wilderness", "Wilderness");

        builder.add("ftbchunks.no_server_mod", "FTB Chunks requires mod on server!");
        builder.add("ftbchunks.already_claimed", "Chunk is already claimed by %s");
        builder.add("ftbchunks.waypoint_added", "Waypoint '%s' added");
        builder.add("ftbchunks.deathpoint_removed", "Waypoint '%s' auto-removed");
        builder.add("ftbchunks.gui.claimed", "Claimed");
        builder.add("ftbchunks.gui.force_loaded", "Force loaded");
        builder.add("ftbchunks.gui.force_load_expires", "Force loading expires");
        builder.add("ftbchunks.gui.allies", "Allies");
        builder.add("ftbchunks.gui.ally_whitelist", "Ally whitelist");
        builder.add("ftbchunks.gui.ally_blacklist", "Ally blacklist");
        builder.add("ftbchunks.gui.large_map", "Large Map");
        builder.add("ftbchunks.gui.claimed_chunks", "Claimed Chunks");
        builder.add("ftbchunks.gui.waypoints", "Waypoint Manager");
        builder.add("ftbchunks.gui.add_waypoint", "Add Waypoint");
        builder.add("ftbchunks.gui.settings", "Settings");
        builder.add("ftbchunks.gui.settings.server", "Server Settings");
        builder.add("ftbchunks.gui.sync", "Share Map with Allies");
        builder.add("ftbchunks.gui.clear_deathpoints", "Clear all Death Waypoints");
        builder.add("ftbchunks.gui.delete_waypoint", "Delete Waypoint '%s' ?");
        builder.add("ftbchunks.gui.change_color", "Change Color");
        builder.add("ftbchunks.gui.hold_alt_for_dates", "Hold Alt: show absolute times");
        builder.add("ftbchunks.gui.mouse_wheel_expiry", "Mouse Wheel: adjust force-load expiry");
        builder.add("ftbchunks.gui.teleport", "Teleport");
        builder.add("ftbchunks.gui.large_map_info", "Key/Mouse Reference");
        builder.add("ftbchunks.gui.large_map_info.text", "Mouse\nLeft Button;Click/Drag to move map view\nRight Button;Context Menu\nMouse Wheel;Rotate to zoom\n\nKeys\nSpace;Center view on player\nC;Open chunk claim screen\nT;Teleport to point (op required)\nS;Open settings screen\nCtrl + S;Open server settings screen (op required)\nF3+G;Toggle Chunk Grid");
        builder.add("ftbchunks.gui.chunk_info", "Chunk Claiming Reference");
        builder.add("ftbchunks.gui.chunk_info.text", "Claiming\nLeft Button;Drag to claim an area\nRight Button;Drag to unclaim an area\n\nForceloading\nShift + Left Button;Drag to forceload an area\nShift + Right Button;Drag to unforceload an area\nMouse Wheel;Rotate on forceloaded chunk to adjust auto-expiry\n\nMisc\nTab;Hold to hide chunk grid\nAlt;Hold to show absolute chunk claim/force times");
        builder.add("ftbchunks.gui.delete", "Delete");
        builder.add("ftbchunks.gui.quick_delete", "Quick Delete");
        builder.add("ftbteamsconfig.ftbchunks", "FTB Chunks Properties");

        addConfig(builder, FTBChunksWorldConfig.ALLOW_FAKE_PLAYERS, "Allow All Fake Players", "Treat ALL fake players as allies of the team.\\nWARNING: Setting this to true could allow hostile players to access your claims via any fake player. Set this to false if you're unsure.");
        addTeamProperty(builder, FTBChunksProperties.ALLOW_NAMED_FAKE_PLAYERS, "Allow Named Fake Players", "Treat these fake players as allies of the team.\nWARNING: Adding entries here could allow hostile players to access your claims via those fake players. Leave this empty if you're unsure.");
        addTeamProperty(builder, FTBChunksProperties.ALLOW_FAKE_PLAYERS_BY_ID, "Allow Fake Players by Player ID", "Allows fake players which have the ID of a real player access to your claims, IF that real player would be permitted, either as ally or team member. Set this to true if you're unsure.");
        addTeamProperty(builder, FTBChunksProperties.ALLOW_EXPLOSIONS, "Allow Explosion Damage", "Should explosions be able to damage blocks in claimed areas?");
        addTeamProperty(builder, FTBChunksProperties.ALLOW_PVP, "Allow PvP Combat", "Should player-vs-player combat be allowed in claimed areas?\nServer config setting 'Allow PvP Combat' must be 'per_team' for this to function\nNot guaranteed to protect against 100% of indirect attacks; requires that damage sources can be attributed to a player");
        addTeamProperty(builder, FTBChunksProperties.ALLOW_MOB_GRIEFING, "Allow Mob Griefing Actions", "Should mobs be allowed to damage blocks in claimed areas?\nNote: currently Endermen only; may include other mobs in future\nCreeper explosions are protected against via \"Allow Explosions\"");
        addTeamProperty(builder, FTBChunksProperties.BLOCK_EDIT_AND_INTERACT_MODE, "Block Edit/Interact Mode", "Used when blocks are being placed, broken, or interacted with");
        addTeamProperty(builder, FTBChunksProperties.BLOCK_EDIT_MODE, "Block Edit Mode", "Used when blocks are being placed or broken");
        addTeamProperty(builder, FTBChunksProperties.BLOCK_INTERACT_MODE, "Block Interact Mode", "Used when blocks are right-clicked, e.g. opening a chest or flipping a lever");
        addTeamProperty(builder, FTBChunksProperties.ENTITY_INTERACT_MODE, "Entity Interact Mode", "Used when entities are right-clicked");
        addTeamProperty(builder, FTBChunksProperties.NONLIVING_ENTITY_ATTACK_MODE, "Non-living Entity Attack Mode", "Used when non-living entities (paintings, item frames etc.) are left-clicked");
        addTeamProperty(builder, FTBChunksProperties.CLAIM_VISIBILITY, "Claim Visibility", "Controls who can see your claims on the map or minimap");
        addTeamProperty(builder, FTBChunksProperties.LOCATION_MODE, "Location Visibility", "Controls who can see you on the map or minimap (outside the normal vanilla tracking range)");

        builder.add("ftbteamsconfig.ftbchunks.minimap_mode", "Minimap Mode");
        builder.add("ftbchunks.fake_players", "Fake Players");
        builder.add("ftbchunks.claiming", "Chunk Claiming");
        builder.add("ftbchunks.force_loading", "Force-Loading");

        addConfig(builder, FTBChunksWorldConfig.MAX_CLAIMED_CHUNKS, "Max Claimed Chunks per Player", "This default can be overridden by the FTB Ranks 'ftbchunks.max_claimed` permission node.");
        addConfig(builder, FTBChunksWorldConfig.MAX_FORCE_LOADED_CHUNKS, "Max Forcedloaded Chunks per Player", "This default can be overridden by the FTB Ranks 'ftbchunks.max_force_loaded` permission node.");
        addConfig(builder, FTBChunksWorldConfig.FORCE_LOAD_MODE, "Offline Forceloading Mode", "NEVER: only allow chunk force-loading if the owning team has at least one online player\nALWAYS: always allow force-loading, even if no players are online\nDEFAULT: allow force-loading IF the team has at least one player with the FTB Ranks 'ftbchunks.chunk_load_offline' permission");
        addConfig(builder, FTBChunksWorldConfig.DISABLE_PROTECTION, "Disable Claim Protection", "Useful for private servers where everyone is trusted, and claims are only used for force-loading");
        addConfig(builder, FTBChunksWorldConfig.PVP_MODE, "Allow PvP Combat in Claimed Chunks", "ALWAYS: allow PvP combat in all claimed chunks\nNEVER: prevent PvP in all claimed chunks\nPER_TEAM: teams can decide if PvP is allowed in their claims via team property");
        addConfig(builder, FTBChunksWorldConfig.ALLY_MODE, "Allow Player to Change Ally Settings", "DEFAULT: FTB Teams ally status is checked to decide if players are allied\nFORCED_ALL: all players are always considered to be allied\nFORCED_NONE: no players are ever considered to be allied");
        addConfig(builder, FTBChunksWorldConfig.CLAIM_DIMENSION_BLACKLIST, "Dimension Blacklist", "Blacklist for dimension ID's where chunks may not be claimed.\nE.g. add \"minecraft:the_end\" to this list if you want to disable chunk claiming in The End\nWildcards are allowed, e.g. \"othermod:*\" matches all dimensions added by \"othermod\"");
        addConfig(builder, FTBChunksWorldConfig.CLAIM_DIMENSION_WHITELIST, "Dimension Whitelist", "Whitelist for dimension ID's where chunks may be claimed. If non-empty, dimension *must* be in this list (and also not in \"Dimension Blacklist\".\nSame syntax as for \"Dimension Blacklist\"");
        addConfig(builder, FTBChunksWorldConfig.NO_WILDERNESS, "Protect Unclaimed Regions", "If true, chunks must be claimed before they can be built on");
        addConfig(builder, FTBChunksWorldConfig.NO_WILDERNESS_DIMENSIONS, "Protect Unclaimed Regions Per-Dimension", "List of dimension ID's where chunks must be claimed before modifying.\nE.g. add \"minecraft:the_nether\" to require chunks to be claimed in the Nether.\nWildcards are allowed, e.g. \"othermod:*\" matches all dimensions added by \"othermod\"");

        addConfig(builder, FTBChunksWorldConfig.FORCE_DISABLE_MINIMAP, "Disable Minimap for Clients");
        addConfig(builder, FTBChunksWorldConfig.MAX_IDLE_DAYS_BEFORE_UNCLAIM, "Max Idle Days Before Unclaim", "If no team member logs in for this many days, the team's claims will be released.\nSetting this to 0 disables auto-unclaiming.");
        addConfig(builder, FTBChunksWorldConfig.MAX_IDLE_DAYS_BEFORE_UNFORCE, "Max Idle Days Before Unforceload", "If no team member logs in for this many days, any force-loaded chunks will no longer be force-loaded.\nSetting this to 0 disables auto-unforceloading.");
        addConfig(builder, FTBChunksWorldConfig.LONG_RANGE_TRACKER_INTERVAL, "Long Range Player Tracker Interval", "Interval in ticks to send updates to clients with long-range player tracking data.\nLower values mean more frequent updates but more server load and network traffic; be careful with this, especially on busy servers.\nSetting this to 0 disables long-range tracking.");
        addConfig(builder, FTBChunksWorldConfig.PROTECT_UNKNOWN_EXPLOSIONS, "Prevent Explosions from Unknown Sources", "Some explosion sources (e.g. Ghasts) can't be determined in code.\nIf this setting is true, damage from these explosion is prevented in protected chunks.");
        addConfig(builder, FTBChunksWorldConfig.HARD_TEAM_CLAIM_LIMIT, "Hard Max Team Claim Limit", "Hard claim limit for party teams, regardless of member count or Party Limit Calculation mode.\nDefault of 0 means no hard limit.");
        addConfig(builder, FTBChunksWorldConfig.HARD_TEAM_FORCE_LIMIT, "Hard Max Team Forceload Limit", "Hard force-load limit for party teams, regardless of member count or Party Limit Calculation mode.\nDefault of 0 means no hard limit.");
        addConfig(builder, FTBChunksWorldConfig.PARTY_LIMIT_MODE, "Party Limit Calculation Mode", "Method by which party claim & force-load limits are calculated.\nLARGEST: use the limits of the member with the largest limits\nSUM: add up all the members' limits\nOWNER: use the party owner's limits only\nAVERAGE: use the average of all members' limits.");
        addConfig(builder, FTBChunksWorldConfig.REQUIRE_GAME_STAGE, "Require Game Stage for Mapping", "If true, players must have the 'ftbchunks_mapping' game stage (KubeJS and/or Gamestages required) to be able to open the map or see the minimap");
        addConfig(builder, FTBChunksWorldConfig.LOCATION_MODE_OVERRIDE, "Override Team \"Location Visibility\"", "If true, team \"Location Visibility\" settings are ignored, and all players can see each other anywhere on the map");
        addConfig(builder, FTBChunksWorldConfig.MAX_PREVENTED_LOG_AGE, "Fake Player Prevented Access Log Age", "Age in days to keep logs of prevented fake player access\nNote: not fully implemented feature; will be used in future to display & control access by fake players to your claims");

        builder.add("ftbchunks.claim_result", "Chunks modified: %d / %d");
        builder.add("ftbchunks.claim_result.other", "Unknown issues");
        addNameMap(builder, ClaimResult.StandardProblem.NAME_MAP, standardProblem -> switch (standardProblem) {
            case NOT_OWNER -> "Not the chunk owner";
            case NOT_ENOUGH_POWER -> "Chunk limit reached";
            case ALREADY_CLAIMED -> "Chunk already claimed";
            case DIMENSION_FORBIDDEN -> "Claiming forbidden in this dimension";
            case NOT_CLAIMED -> "Chunk not claimed";
            case ALREADY_LOADED -> "Chunk already loaded";
            case NOT_LOADED -> "Chunk not loaded";
        });
        builder.add("ftbchunks.need_to_claim_chunk", "You need to claim this chunk to interact with blocks here!");

        builder.add("ftbchunks.label.show", "Show");
        builder.add("ftbchunks.label.hide", "Hide");
        builder.add("ftbchunks.message.no_pvp", "PvP combat is disabled here");
        builder.add("ftbchunks.game_time", "Game Time: %s");
        builder.add("ftbchunks.real_time", "Real Time: %s");
        builder.add("ftbchunks.fps", "FPS: %d");
        builder.add("ftbchunks.waypoint.shared", "Has shared waypoint '%s' with you! Click to add");
        builder.add("ftbchunks.waypoint.shared_by_you", "You shared waypoint '%s' !");
        builder.add("ftbchunks.waypoint.share", "Share");
        builder.add("ftbchunks.waypoint.share.server", "Server");
        builder.add("ftbchunks.waypoint.share.party", "Party");
        builder.add("ftbchunks.waypoint.share.player", "Player");
        builder.add("ftbchunks.waypoint_sharing", "Waypoint Sharing");
        builder.add("ftbchunks.waypoint_sharing.waypoint_sharing_party", "Allow Sharing waypoints with Party");
        builder.add("ftbchunks.waypoint_sharing.waypoint_sharing_server", "Allow Sharing waypoints with Server");
        builder.add("ftbchunks.waypoint_sharing.waypoint_sharing_players", "Allow Sharing waypoints with Players");
        builder.add("ftbchunks.gui.move_up", "Move Up");
        builder.add("ftbchunks.gui.move_down", "Move Down");
        builder.add("ftbchunks.gui.toggle_visibility_off", "Toggle Visibility - Off");
        builder.add("ftbchunks.gui.toggle_visibility_on", "Toggle Visibility - On");
        builder.add("ftbchunks.gui.sort_minimap_info", "Minimap Info Settings");
        builder.add("ftbchunks.minimap.info_hidden", "Hidden Minimap Info");
        builder.add("ftbchunks.minimap.info_order", "Minimap info order");
        builder.add("ftbchunks.show_wilderness.show_wilderness", "Show Wilderness");
        builder.add("ftbchunks.show_wilderness.just_claimed", "Show only Claimed Chunks");

        addNameMap(builder, BiomeBlendMode.NAME_MAP, biomeBlendMode -> switch (biomeBlendMode) {
            case NONE -> "None (Fastest)";
            case BLEND_15X15 -> "Blend 15x15 (Slowest)";
            default -> capitalizeEnum(biomeBlendMode.name());
        });
        addNameMap(builder, MapMode.NAME_MAP, mapMode -> capitalizeEnum(mapMode.name()));
        addNameMap(builder, AllyMode.NAME_MAP, allyMode -> capitalizeEnum(allyMode.name()));
        addNameMap(builder, PvPMode.NAME_MAP, pvpMode -> capitalizeEnum(pvpMode.name()));
        addNameMap(builder, PartyLimitMode.NAME_MAP, partyLimitMode -> capitalizeEnum(partyLimitMode.name()));
        addNameMap(builder, ForceLoadMode.NAME_MAP, forceLoadMode -> capitalizeEnum(forceLoadMode.name()));
        addNameMap(builder, RealTimeComponent.TimeMode.NAME_MAP, timeMode -> capitalizeEnum(timeMode.name()));
        addNameMap(builder, MinimapBlurMode.NAME_MAP, minimapBlurMode -> capitalizeEnum(minimapBlurMode.name()));
        addNameMap(builder, MinimapPosition.NAME_MAP, minimapPosition -> capitalizeEnum(minimapPosition.name()));
        addNameMap(builder, MinimapPosition.MinimapOffsetConditional.NAME_MAP, minimapOffsetConditional -> capitalizeEnum(minimapOffsetConditional.name()));

        addMinimapInfo(builder, BiomeComponent.ID, "Biome", "Render Biome");
        addMinimapInfo(builder, FPSComponent.ID, "FPS", "Render FPS");
        addMinimapInfo(builder, GameTimeComponent.ID, "Game Time", "Render Game Time");
        addMinimapInfo(builder, DebugComponent.ID, "Debug", "Render Debug");
        addMinimapInfo(builder, PlayerPosInfoComponent.ID, "Player Pos", "Render Player Pos");
        addMinimapInfo(builder, RealTimeComponent.ID, "Real Time", "Render Real Time");
        addMinimapInfo(builder, ZoneInfoComponent.ID, "Zone", "Render Zone");

        builder.add(FTBChunksTags.Items.RIGHT_CLICK_BLACKLIST_TAG, "Right Click Blacklist");
        builder.add(FTBChunksTags.Items.RIGHT_CLICK_WHITELIST_TAG, "Right Click Whitelist");
    }

    public void addTeamProperty(TranslationBuilder builder, TeamProperty<?> property, String value, String tooltip) {
        addWithTooltip(builder, property.getTranslationKey("ftbteamsconfig"), value, tooltip);
    }

    public void addWithTooltip(TranslationBuilder builder, String key, String value, String tooltip) {
        tryAdd(builder, key, value);
        tryAdd(builder, key + ".tooltip", tooltip);
    }

    public void addMinimapInfo(TranslationBuilder builder, ResourceLocation id, String title, String description) {
        tryAdd(builder, "minimap.info." + id.getNamespace() + "." + id.getPath() + ".title", title);
        tryAdd(builder, "minimap.info." + id.getNamespace() + "." + id.getPath() + ".description", description);
    }

    public <T> void addConfig(TranslationBuilder builder, BaseValue<T> config, String value) {
        tryAdd(builder, config.toString().replace("/", ".").replace("-client", "").replace("-world", ""), value);
    }

    public <T> void addConfig(TranslationBuilder builder, BaseValue<T> config, String value, String tooltip) {
        String key = config.toString().replace("/", ".").replace("-client", "").replace("-world", "");
        builder.add(key, value);
        if(tooltip != null) {
            tryAdd(builder, key + ".tooltip", tooltip);
        }
    }

    public <E> void addNameMap(TranslationBuilder builder, NameMap<E> nameMap, Function<E, String> value) throws IllegalStateException {
        for (E e : nameMap.values) {
            Component displayName1 = nameMap.getDisplayName(e);
            if (displayName1.getContents() instanceof TranslatableContents contents) {
                tryAdd(builder, contents.getKey(), value.apply(e));
            }
        }
    }

    public void tryAdd(TranslationBuilder builder, String key, String value) {
        try {
            builder.add(key, value);
        }catch (RuntimeException ignored) {

        }
    }

    public String capitalizeEnum(String name) {
        StringBuilder sb = new StringBuilder();
        for (String s : name.split("_")) {
            sb.append(s.substring(0, 1).toUpperCase()).append(s.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }
}
