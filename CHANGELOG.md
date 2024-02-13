# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2001.2.6]

### Fixed
* Setting `disable_protection` to false in server config `ftbchunks-world.snbt` now allows explosions to happen in claimed chunks

## [2001.2.5]

### Added
* Added `/ftbchunks waypoint add <name> <pos> [<color>]` command, to add waypoints from server-side
  * Name can contain spaces if it's quoted
  * Pos is a standard blockpos spec, e.g. `~ ~ ~` for the player's current pos
  * Color is optional; either a chat color or, if omitted, a random color is picked

### Fixed
* Fixed a couple of claim and force load limit issues
  * Max claim/force limits weren't synced to clients when changed in server config
  * Team claim/force limits weren't always correctly recalculated when a player joined/left a team

## [2001.2.4]

### Fixed
* Fixed cross-mod compat issue causing an NPE
  * not entirely sure of the cause, but added some defensive coding checks to ensure chunk claim manager is not null when it's used in a mixin

## [2001.2.3]

### Added
* Added a team property to control whether PvP is permitted in a team's claims
  * If PvP is prevented, then if either the attacking player or the attacked player is in such a claim, PvP damage will be cancelled
  * Can be controlled by server admin with the server config "Allow PvP Combat"
    * "always" (default) allows PvP everywhere
    * "never" prevents PvP in all claimed chunks
    * "per_team" allows teams to configure PvP for their claims via new team property "Allow PvP Combat"
  * Not 100% guaranteed to prevent all forms of PvP damage, but direct or projectile damage is prevented where the damage source can be traced back to a player

### Fixed
* Fixed a held item desync when an item or block right-click is prevented in a claimed area
* Cleaned up stale kubjes support files (kubejs.classfilter.txt / kubejs.plugins.txt) - they're in FTB XMod Compat now

## [2001.2.2]

### Fixed
* Fixed crash on player disconnect if they don't have a team assigned
  * Should never occur under normal circumstances but could happen if external factors force a premature disconnect

## [2001.2.1]

### Fixed
* Fixed "Show waypoints in world" client config setting being ignored for waypoint icons
  * It was only working to suppress beacons when set to false, now it suppresses icons too
* Fixed NPE when checking for fake players which had a null name or UUID in their GameProfile
* Client memory fix; eliminated some unnecessary region data loading when players change chunk settings (claiming, forceloading)

## [2001.2.0]

### Changed
* All cross-mod integration has been moved to the **FTB XMod Compat** mod
  * This includes: FTB Ranks / Luckperms, Waystones, Game Stages (Forge only) and Common Protection API (Fabric only)

## [2001.1.5]

### Fixed
* Fixed cache invalidation issue when (un)claiming/loading chunks which caused client/server desync

## [2001.1.4]

### Fixed
* Fixed a NoSuchMethodError crash with Game Stages mod
* Worked around Optifine bug, which is firing Forge mob griefing events on the client and causing client crashes

## [2001.1.3]

### Fixed
* Fixed Endermen still being able to grief claimed chunks
  * Related, fixes crashes with recent Forge releases (event cancelling semantics have changed for the mob griefing event)

## [2001.1.2]

### Added
* Updated to MC 1.20.1, based on 1902.3.22 release

## [1902.3.22]

### Added
* The claim manager screen can now be opened without first opening the large map screen
  * Added a new sidebar button "FTB Chunks: Claim Manager" to open the claim manager screen
  * Also added a new key binding to open the claim manager, not bound to any key by default
* Added Luckperms support, as an alternative to FTB Ranks
  * Same permission node names: "ftbchunks.max_claimed", "ftbchunks.max_force_loaded", "ftbchunks.chunk_load_offline", "ftbchunks.no_wilderness"
* The minimap is now sized as a proportional of the current screen width
  * Base size is 10% of the screen width, although this is modifiable with the existing "Scale" client setting
  * Added new "Proportional Sizing" client setting, true by default. Set this to false if you prefer the old behaviour (fixed-size minimap regardless of screen resolution)

### Fixed
* Fixed occasional (non-fatal) NPE which could be thrown in the client block scanning thread
* Made chunks owned by server teams exempt from claim and forceload auto-expiry (server teams are admin-level)

## [1902.3.21]

### Added
* Fabric: FTB Chunks now acts as a protection provider for [Patbox's Common™ Protection API](https://github.com/Patbox/common-protection-api)
  * Mods which support the Common Protection API will now benefit from FTB Chunks claim protection
* Player arrow icon is now shown on the minimap even when minimap rotation is not locked
  * Added new "Show Player When Unlocked" client config setting to toggle this behaviour
* Added mob griefing protection (Enderman only for now, but maybe more in future)
  * New "Mob Griefing" boolean property, which can be changed in the team properties manager (top right button in the FTB Teams window)
  * True by default; prevents Endermen taking or placing blocks in claimed chunks
* Large map screen now supports more hotkeys, and hotkey tooltips have been added to buttons on this screen as appropriate
  * "S" opens the settings GUI
  * "Ctrl + S" opens the server settings GUI, if the player has permission
  * The "Waypoint Manager" keybind now works in the large map screen too
  * "C" still opens the chunk claiming GUI
* In-world waypoint dots can now be seen at any range; no more arbitrary cut-off at around 750 blocks
  * Added new "Waypoints: max draw distance" setting in client config to control the maximum range
  * Note: the vertical beacon still fades out when more than a couple of hundred blocks away
* Added some missing face icons for various vanilla entities (mostly new 1.19 mobs, but some older ones too)
* Added "Override Team Location Visibility" boolean server config setting, default false
  * When true, all players can see everyone on the map, regardless of team location visibility preferences
* Added "Dimension Whitelist" setting to server config (in addition to existing "Dimension Blacklist")
  * If whitelist is not empty, *only* dimension ID's in the whitelist may have chunks claimed, and only if those dimensions are not in the blacklist
  * Wildcarded dimensions are now supported too, e.g. `somemod:*` matches all the dimensions added by the mod `somemod`
* When chunks are claimed/unclaimed/forceloaded/unforceloaded in the chunk GUI, feedback is now given on how many chunks were modified
  * Also shows the reasons why any chunks could not be modified (e.g. dimension blacklisted, chunk owned by someone else...)

### Fixed

* Some significant client-side memory management work has been done
  * Addressed some conditions which could lead to client-side memory starvation when the game has been running for a while
  * Specifically, well-explored worlds where the player is either moving around the world a lot, or viewing the world with high map zoom-out
  * Periodically, least-recently accessed region data is released from RAM, requiring reload from disk on the next access. Every 300 seconds by default; can be tuned in client config.
  * When the large map screen is closed, regions furthest from the player are released from RAM, down to 32 loaded regions by default; also tunable in client config.
  * Map zoom-out is limited where the ratio of the number of known (explored) regions to available JVM memory is poor. Limiting zoom-out reduces the number of regions which need to be loaded in memory at a given moment. This can be disabled in client config if you prefer.
  * New client config settings are available in the "Memory Usage" section of the client config; tuning them is a trade-off between RAM usage and disk activity. However, even when tuned toward lower RAM usage, the level of disk activity should not be a major concern. 

## [1902.3.20]

### Fixed

* Issues with FTB Library

### Changed

* Bumped minimum version for FTB Library

## [1902.3.19]

### Added
* Architectury >= 6.5.77 is now a requirement
  * This version of Architectury fixes some block break event timing issues leading to dupes on Fabric

## Fixed
* Fixed a crash with fake player mods which use buckets to pick up water in protected chunks
* Fixed interaction with Fabric mods which do block placement protection by firing the FAPI block break event directly (thanks @TelepathicGrunt)
  * Example mod: Bumblezone 
  * Architectury currently handles this via its own mixin


## [1902.3.18]

### Fixed
- Fixed an NPE which can occur in conjunction with some mods' fake player objects

## [1902.3.17]

### Fixed
- Fixed a problem on Fabric (Forge not affected) with block break permissions in protected chunks

## [1902.3.16]

### Added
- New team property "Non-living Entity Attack Mode", used when left-clicking non-living entities like item frames or paintings
- The `max_idle_days_before_unforce` and `max_idle_days_before_unclaim` server config settings can now be floating-point values
- Fade distances for waypoint beacons and waypoint dots can now be configured independently
  - Previously in-game dot icons for waypoints never faded out, regardless of proximity
  - New client-side setting "Waypoint dot fade distance", default 1 block away
  - "Waypoint fade distance" is now "Waypoint beacon fade distance" (default still 12 blocks)

### Fixed
- Fixed a block placement dupe issue on Fabric
  - On Fabric, there is now only a single "Block Interaction and Edit" team property, since it isn't possible to reliably distinguish between right-clicking a block to use vs. right-clicking a block to place another block
  - Forge functionality is unchanged
- Re-sync player's held item when a block placement fails due to protected claims
- Fixed another chunkloading issue on Forge where stale tickets weren't always being cleaned up on server restart

## [1902.3.15]

### Added
- The handling of Fake Player access to protected chunks has been improved
  - New team property "Allow Fake Players by Player ID", default true. This is the secure way to allow fake player access to your claims, but it depends on mods actually giving their fake players the ID of the real player deploying that fake player. Examples of mods which do this properly are the Mekanism Digital Miner, PneumaticCraft: Repressurized Drones, and Modular Routers (with an installed Security Upgrade).
  - Team property "Allow Fake Players" is now "Allow All Fake Players", and is false by default (you may wish to review this setting for your team). Beware: setting this to true treats ALL fake players as team allies, including those from blocks/entities owned by potentially hostile players!
  - New team property "Allied Fake Player Names/IDs": you can add names or IDs of known fake players. Beware: adding names or IDs to this list treats these fake players as team allies, even from blocks/entities owned by potentially hostile players!
- Right-clicking corpse entities from the "Corpse" mod is now permitted in any claimed chunk
  - the `corpse:corpse` entity type is added to the `ftbchunks:entity_interact_whitelist` entity type tag
- Sharestones from the Waystones mod, and all waystones from the Fabric Waystones mods can now be interacted with in claimed chunks
  - added some more entries to the `ftbchunks:block_interact_whitelist` block tag
- Added new server-side config setting `max_idle_days_before_unforce`
  - If no member of a team logs in for this many days, any force-loaded chunks owned by the team will become un-forceloaded
  - Default is 0, meaning no un-forceloading will be done; server admins should set this to a value suiting their server
- Added new protection team property: "Non-living Entity Attack Mode"
  - Allows for protection from left-clicking of non-living entities like Item Frames and Paintings in your base
  - Does *not* prevent living entities from being attacked

### Fixed
- Fixed NPE when a block break event is received with bad level data in it
- Fixed player head icons in the previous dimension not disappearing from the map or minimap when you change dimension
  - Only players on long-range tracking (i.e. outside normal vanilla entity tracking range)
- Fixed chunkloading issue on Forge where some forceloaded tickets weren't being cleared when offline chunkloading is disabled for a team
  - Causing chunks to sometimes stay loaded when they should not be

## [1902.3.14]

### Added
- Gamestage support for map and minimap usage
  - New `require_game_stage` server setting, default false. If true, players must have the `ftbchunks_mapping` stage to view the map or minimap
  - Requires KubeJS (Forge or Fabric) and/or Gamestages (Forge only) to be installed

### Fixed
- Fixed player death waypoints being added in the wrong place on Fabric
- Fix force-loaded chunks not always ticking entities & block entities in those chunks

## [1902.3.13]

### Added
* Server settings are now editable via a new button in the map GUI (bottom right), for players with permission level >= 2

### Fixed
* Fixed a server CME crash under some circumstances when players disconnect

## [1902.3.12]

### Fixed
* Fixed NPE sometimes occurring on player login with FTB Ranks installed

## [1902.3.11]

### Fixed
* Fixed NPE sometimes occurring on player login (getting player's team too early)

## [1902.3.10]

### Added

* Square minimap option (see "Square Minimap" in client options, default false)
* Added ability for teams to specify new visibility settings in Team settings
  * "Location Visibility" determines how player heads are visible to players in other teams on the map (default: Allies)
  * "Claim Visibility" determines how chunk claims are visible to other teams on the map (default: Public)
  * This means player heads will only be visible to team-mates and allies by default; if you're not in a party team but want to be visible on the map, set your "Location Visibility" to "Public" in team settings.
* Player heads can now be tracked on the map at any range, not just inside the default entity tracking range
  * The same visibility restrictions apply as above, via "Location Visibility"
  * Added server-side config item `long_range_tracker_interval` which controls how frequently long-range tracking data is sent by the server to clients; default is every 20 ticks (for players who are moving). 
  * Set this to 0 to disable long-tracking entirely
  * Be careful about setting this to very low (non-zero) values; it can cause extra server and network load, especially on very busy servers
* Added entity interaction protection as a Team setting
  * Controls the ability for non-team-members to interact with e.g. Armor Stands, Item Frames and other entities
  * Support for entity interaction whitelisting via the `ftbchunks:entity_interact_whitelist` entity type tag
* Added hotkey 'C' to quickly switch between large map GUI and chunk claiming GUI
* Added a hotkey (not bound by default) to quickly add a waypoint at the player's current position
* Added a client configurable distance whereby death waypoints are automatically removed if the player gets close enough
  * Default of 0 means death waypoints are not auto-removed
* Added a waypoint manager GUI, accessible via a button on the large map GUI, or via hotkey (unbound by default)
  * Can be used to view all of your waypoints, sorted by dimension and distance, toggle visibility, change color & label, and delete them
* Added server configurable timeout whereby a team's chunk claims are automatically released if no one in the team logs in
  * Default of 0 days means no automatic claim loss
  * Intended to prevent claims owned by teams who no longer play hanging around forever
  * See `max_idle_days_before_unclaim` in server config file
* Beneficial splash and lingering potions may be used by any player in any chunk, regardless of claim protection
* A player's original chunk claims are now remembered when they join a team and returned to them if they leave the team
  * Prevents claim stealing by maliciously inviting then kicking a player from a team
  * This doesn't apply retrospectively; claims made before this release of the mod are not remembered in this way
* Added ability to temporarily force-load a chunk via chunk claiming GUI
  * Can use the mouse wheel on force-loaded chunks owned by your team to adjust a force-load time for the chunk
  * Chunk will be automatically unforced within 10 minutes of this time expiring (but kept claimed, of course)
* The way team claim and forceload limits is calculated has been significantly reworked
  * Previously the team owner's limits were used, leading to confusion if a team member with higher limits joined
  * Now there is a server configurable `party_limit_mode` to control this
  * LARGEST (default): use the limits of the team member with the largest limits
  * SUM: team limit is the sum of all team members' limits
  * OWNER: use the owner limits only (old behaviour)
  * AVERAGE: use an average of all members' limits
  * Note that limits can't be fully calculated until players actually log in
  * Also added `hard_team_claim_limit` and `hard_team_force_limit` server configurables; if set to a non-zero value, these hard limits apply regardless of calculated team limits
* Integration with the Waystones mod has been added back

### Fixed
* Fixed fake death markers sometimes being created (generally when some other mod cancelled a player death)
* Fixed forced chunks sometimes staying loaded when they shouldn't (e.g. if all players in a team have logged out and offline force-loading is disabled)
* Fixed forced chunks sometimes not getting loaded when they should (e.g. player logs in when offline-forceloading is disabled)
* Fixed team data not getting properly saved to disk when a player leaves a party team
* Fixed deleted party team data not getting purged from disk
* Fixed Ghast fireballs being able to grief protected chunks
  * A more fundamental problem here is that not all explosions sources can be determined, and Ghast fireballs fall into this category. Previously such explosions were not protected against but are now.
  * Added a server-side config item `protect_unknown_explosions` (default true) to control this behaviour
