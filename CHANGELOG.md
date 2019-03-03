Changelog

v1.2.0
---

- Add a second recruiter
- Move recruiters around independent worlds more reliably
- Vesperon cache reps spawn/despawn dynamically on worlds run by Independent authorities
- ...and also player colonies, for extra convenience :)

1.1.1
---

- Updated available vanilla blueprint whitelist to use fighter wing IDs, not fighter hull IDs
- Prevent Remnant and BB spawn breakage

1.1.0
---

- Added whitelisting system for other mods to opt-in
    - Add to `data/config/vesperon_blueprints.json` to whitelist content
    - See example vanilla config file
- Whitelisted all (obtainable) vanilla blueprints
- Slightly optimised VesperonIntelManager to not load new JSON structs all the time
- Move the Vesperon rep around once every 60 days to make sure market decivs (et al) can't break the mod

1.0
---

- Add a bar event to initiate membership 
- Add agents to independent market comm directories
- Add a bit of background lore
- Add hidden orbital blueprint caches containing a random selection of blueprints the player does not have
    - Stations should only appear in procgen systems / with no markets (for Nex random sector support)
    - Add 'special' automated defenses with unique ability
    - Add a chance of \[REDACTED\]
    - Filtered some blueprints
    - Prevent boarding when hostile fleets or orbiting fleets are active
- A small hint of things to come :)