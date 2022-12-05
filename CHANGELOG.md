# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1802.3.8]

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