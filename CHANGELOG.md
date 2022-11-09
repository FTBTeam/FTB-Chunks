# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
* Square minimap option (see "Square Minimap" in client options, default false)
* Added ability for teams to specify new visibility settings in Team settings
  * "Location Mode" determines how player heads are visible to players in other teams on the map
  * "Claim Visibility" determines how chunk claims are visible to other teams on the map
* Added entity interaction protection as a Team setting
  * Controls the ability for non-team-members to interact with e.g. Armor Stands, Item Frames and other entities
  * Support for entity interaction whitelisting via `ftbchunks:entity_interact_whitelist`
* Added a hotkey (not bound by default) to quickly add a waypoint at the player's current position
* Added a client configurable distance whereby death waypoints are automatically removed if the player gets close enough
  * Default of 0 means death waypoints are not auto-removed
* Added server configurable timeout whereby a team's chunk claims are automatically released if no one in the team logs in
  * Default of 0 days means no automatic claim loss
  * Intended to prevent claims owned by teams who no longer play hanging around forever
* Beneficial splash and lingering potions may be used by any player in any chunk, regardless of claim
* A player's original chunk claims are now remembered when they join a team and returned to them if they leave the team
  * Prevents claim stealing by maliciously inviting then kicing a player from a team
* Integration with the Waystones mod has been added back

### Fixed
* Fixed fake death markers sometimes being created (generally when some other mod cancelled a player death)
* Fixed forced chunks sometimes staying loaded when they shouldn't (e.g. if all players in a team have logged out and offline force-loading is disabled)
* Fixed forced chunks sometimes not getting loaded when they should (e.g. player logs in when offline-forceloading is disabled)
* Fixed team data not getting properly saved to disk when a player leaves a party team
* Fixed deleted party team data not getting purged from disk
