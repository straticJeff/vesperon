package org.stratic.fs.starsector.api.impl.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.DropGroupRow;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BaseSalvageSpecial;
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidBeltTerrainPlugin;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.ListMap;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;
import org.stratic.fs.starsector.api.impl.campaign.ids.VesperonHullMods;
import org.stratic.fs.starsector.api.impl.campaign.ids.VesperonTags;
import org.stratic.fs.starsector.api.impl.campaign.ids.VesperonTags.VesperonCache;
import org.stratic.fs.starsector.api.impl.campaign.ids.VesperonTags.VesperonFacilityCondition;
import org.stratic.fs.starsector.api.impl.campaign.ids.VesperonTags.VesperonHazard;
import org.stratic.fs.starsector.util.SequenceGenerator;

import java.io.IOException;
import java.util.*;

public class VesperonIntelManager {

    public enum BlueprintFilterMode {
        FILTER_MODE_WHITELIST,
        FILTER_MODE_TAGS,
        FILTER_MODE_NONE,
    }

    private BlueprintFilterMode filterMode;

    private static final String SEQUENCE_GENERATOR_KEY = "VesperonSeed";

    private HashMap<String, Set<String>> allFactionBlueprints;
    private HashMap<String, Set<String>> whiteListedBlueprints;

    public class VesperonFacilityLocation {
        StarSystemAPI system;
        CampaignTerrainAPI terrain;
    }

    public static VesperonIntelManager getInstance() {
        BlueprintFilterMode blueprintFilterMode = BlueprintFilterMode.FILTER_MODE_WHITELIST;
        if (!Global.getSector().getMemoryWithoutUpdate().contains(VesperonTags.KEY_MANAGER_INSTANCE)) {
            try {
                JSONObject config = Global.getSettings().loadJSON("data/config/vesperon.json");
                if (config.has("blueprintFilterMode")) {
                    blueprintFilterMode = BlueprintFilterMode.valueOf(config.getString("blueprintFilterMode"));
                    return getInstance(blueprintFilterMode);
                }
            } catch (IOException | JSONException | RuntimeException ignored) {

            }
        }
        return getInstance(blueprintFilterMode);
    }

    private static VesperonIntelManager getInstance(BlueprintFilterMode filterMode) {
        if (Global.getSector().getMemoryWithoutUpdate().contains(VesperonTags.KEY_MANAGER_INSTANCE)) {
            return (VesperonIntelManager)Global.getSector().getMemory().get(VesperonTags.KEY_MANAGER_INSTANCE);
        } else {
            VesperonIntelManager manager = new VesperonIntelManager(filterMode);
            Global.getSector().getMemoryWithoutUpdate().set(VesperonTags.KEY_MANAGER_INSTANCE, manager);
            return manager;
        }
    }

    private VesperonIntelManager(BlueprintFilterMode filterMode) {
        Logger l = Global.getLogger(this.getClass());
         l.info("VesperonIntelManager init: using filter mode " + filterMode.toString());
        this.filterMode = filterMode;

        allFactionBlueprints = new HashMap<>();
        allFactionBlueprints.put(Items.SHIP_BP, new HashSet<String>());
        allFactionBlueprints.put(Items.WEAPON_BP, new HashSet<String>());
        allFactionBlueprints.put(Items.FIGHTER_BP, new HashSet<String>());
        allFactionBlueprints.put(Items.MODSPEC, new HashSet<String>());

        logBlueprintSet(allFactionBlueprints);

        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (
                !faction.isPlayerFaction()
            ) {
                HashMap<String, Set<String>> factionPublicBlueprints = getKnownBlueprintsForFaction(faction);
                Set<String> categoryKeys = allFactionBlueprints.keySet();
                for (String categoryKey : categoryKeys) {
                    Set<String> categoryBlueprints = factionPublicBlueprints.get(categoryKey);
                    Set<String> allCategoryBlueprints = allFactionBlueprints.get(categoryKey);
                    allCategoryBlueprints.addAll(categoryBlueprints);
                }
            }
        }
        logBlueprintSet(allFactionBlueprints);

        if (this.filterMode == BlueprintFilterMode.FILTER_MODE_WHITELIST) {
            loadBlueprintWhitelist();
            for (String key : allFactionBlueprints.keySet()) {
                allFactionBlueprints.get(key).retainAll(whiteListedBlueprints.get(key));
            }
        }
    }


    private void logBlueprintSet(HashMap<String, Set<String>> map) {
        Logger l = Global.getLogger(this.getClass());
        l.info("Vesperon blueprint map keys: " + map.keySet().toString());
        for (String key : map.keySet()) {
            l.info("Key " + key + ": " + map.get(key).toString());
        }
    }

    /**
     * Generates a facility and a mission to find it.
     *
     * @param cache type of cache to generate
     */
    public void createVesperonMission(VesperonCache cache) {
        Logger l = Global.getLogger(this.getClass());
        SequenceGenerator generator = SequenceGenerator.get(SEQUENCE_GENERATOR_KEY);
        HashMap<String, Set<String>> unknownBlueprints = getUnknownBlueprintsForFaction(
            Global.getSector().getPlayerFaction()
        );

        int cacheMaxValue = (int) cache.minValue + generator.nextInt((int) cache.maxValue - (int) cache.minValue);
        int cacheValue = 0;
        int blueprintsEvaluated = 0;

        ArrayList<String[]> unknownBlueprintIds = new ArrayList<>(getAllNamespacedBlueprints(unknownBlueprints));
        l.info("Count of all unknown blueprints IDs: " + unknownBlueprintIds.size());

        int unknownBlueprintCount = unknownBlueprintIds.size();
        HashSet<String[]> cacheBlueprints = new HashSet<>();

        while (cacheValue < cacheMaxValue && blueprintsEvaluated <= unknownBlueprintCount) {
            int commodityIndex = generator.nextInt(unknownBlueprintIds.size());

            String[] commodity = unknownBlueprintIds.get(commodityIndex);
            String commodityPrefix = commodity[0];
            String commodityStem = commodity[1];

            float commodityPrice = getPriceForCommodity(commodityPrefix, commodityStem);

            if (shouldAddCommodity(commodityPrefix, commodityStem) && cacheValue + commodityPrice <= cacheMaxValue) {
                unknownBlueprintIds.remove(commodityIndex);
                cacheBlueprints.add(commodity);

                cacheValue += commodityPrice;
            }

            blueprintsEvaluated++;
        }

        List<VesperonFacilityLocation> suitableLocations = getSuitableTargetsForStation();
        int locationIndex = generator.nextInt(suitableLocations.size() - 1);
        VesperonFacilityLocation facilityLocation = suitableLocations.get(locationIndex);

        Vector2f currentLocation;

        VesperonFacilityCondition level = generateFacilityCondition(cache);
        String entityType = VesperonTags.FACILITY_POOR;
        switch (level) {
            case CONDITION_GOOD:
                entityType = VesperonTags.FACILITY_GOOD;
                break;
            case CONDITION_PRISTINE:
                entityType = VesperonTags.FACILITY_PRISTINE;
                break;
        }
        CustomCampaignEntityAPI facility = facilityLocation.system.addCustomEntity(
            null,
            null,
            entityType,
            Factions.NEUTRAL
        );

        CampaignTerrainAPI terrain = facilityLocation.terrain;
        OrbitAPI o = terrain.getOrbit();
        AsteroidBeltTerrainPlugin plugin = (AsteroidBeltTerrainPlugin) terrain.getPlugin();

        boolean insideField = generator.nextBoolean();
        if (o.getFocus() != null) {
            l.info("Generating orbit for station");
            facility.setCircularOrbit(
                o.getFocus(),
                generator.nextInt(360),
                plugin.params.middleRadius - plugin.params.bandWidthInEngine * 0.75f * (insideField ? -1 : 1),
                terrain.getCircularOrbitPeriod()
            );
        } else {
            l.info("Generating fixed location for station");
            currentLocation = terrain.getLocation();
            facility.setFixedLocation(currentLocation.x, currentLocation.y);
        }

        // convert SpecialItemSpecAPI list to extra salvage
        CargoAPI salvage = Global.getFactory().createCargo(true);
        for (String[] blueprint : cacheBlueprints) {
            salvage.addSpecial(new SpecialItemData(blueprint[0], blueprint[1]), 1);
            l.info("Adding salvage: " + DropGroupRow.ITEM_PREFIX + blueprint[0] + ":" + blueprint[1]);
        }

        facility.setDiscoverable(true);
        facility.setSensorProfile(1000f);
        facility.setSalvageXP(10000f);
        facility.addTag(Tags.HAS_INTERACTION_DIALOG);
        facility.addTag(VesperonTags.BASE);
        facility.addTag(VesperonTags.FACILITY_BASE);
        facility.getMemoryWithoutUpdate().set(
            BaseSalvageSpecial.EXTRA_SALVAGE,
            new BaseSalvageSpecial.ExtraSalvage(salvage)
        );

        Misc.setFlagWithReason(
            facility.getMemoryWithoutUpdate(),
            MemFlags.ENTITY_MISSION_IMPORTANT,
            "vesperon",
            true,
            Integer.MAX_VALUE
        );

        facility.addTag(level.toString());

        assignFacilityHazard(facility);
        assignSpecialAutomatedDefenses(facility);

        VesperonIntel vesperonIntel = new VesperonIntel(facilityLocation.system.getCenter(), facility);
        Global.getSector().getIntelManager().addIntel(vesperonIntel);

    }

    private boolean shouldAddCommodity(String category, String commodityId) {
        switch (category) {
            case Items.FIGHTER_BP:
                return !commodityId.contains("khs_dervish"); // causes a crash
            case Items.SHIP_BP:
            case Items.WEAPON_BP:
            case Items.MODSPEC:
                return true;
        }
        return false;
    }

    private float getPriceForCommodity(String category, String commodityId) {
        float minPrice = 15000f;
        float price = minPrice;
        float basePriceMultiplier = Global.getSettings().getFloat("blueprintPriceOriginalItemMult");
        switch (category) {
            case Items.SHIP_BP:
                // get ship cost from ship manager
                ShipHullSpecAPI shipSpec = Global.getSettings().getHullSpec(commodityId);
                price = shipSpec.getBaseValue();
                break;
            case Items.FIGHTER_BP:
                FighterWingSpecAPI fighterSpec = Global.getSettings().getFighterWingSpec(commodityId);
                price = fighterSpec.getBaseValue();
                break;
            case Items.WEAPON_BP:
                WeaponSpecAPI weaponSpec = Global.getSettings().getWeaponSpec(commodityId);
                price = weaponSpec.getBaseValue();
                break;
            case Items.MODSPEC:
                HullModSpecAPI modSpec = Global.getSettings().getHullModSpec(commodityId);
                price = modSpec.getBaseValue();
                break;
        }
        float finalPrice = price * basePriceMultiplier;
//        finalPrice = 1;
        return (finalPrice > minPrice ? finalPrice : minPrice);
    }

    /**
     * Generates Vesperon mission-giver agents at independent markets.
     */
    public void activateMembershipAgents() {
        List<LocationAPI> allMarketLocations = Global.getSector().getEconomy().getLocationsWithMarkets();
        for (LocationAPI marketLocation : allMarketLocations) {
            List<MarketAPI> markets = Global.getSector().getEconomy().getMarkets(marketLocation);
            for (MarketAPI market : markets) {
                if (market.getFaction().getId().equals(Factions.INDEPENDENT)) {
                    generatePerson(market);
                }
            }
        }
        Global.getSector().getMemory().set(VesperonTags.AGENTS_ADDED, true);
    }

    private HashMap<String, Set<String>> getKnownBlueprintsForFaction(FactionAPI faction) {
        HashMap<String, Set<String>> factionPublicBlueprints = new HashMap<>();

        Set<String> knownShips = faction.getKnownShips();
        for (Iterator<String> iterator = knownShips.iterator(); iterator.hasNext(); ) {
            String shipId = iterator.next();
            ShipHullSpecAPI ship = Global.getSettings().getHullSpec(shipId);
            if (
                shouldExcludeBasedOnHints(ship) ||
                shouldExcludeBasedOnTags(ship)
            ) {
                iterator.remove();
            }
        }
        factionPublicBlueprints.put(Items.SHIP_BP, knownShips);

        Set<String> knownWeapons = faction.getKnownWeapons();
        for (Iterator<String> iterator = knownWeapons.iterator(); iterator.hasNext(); ) {
            String weaponId = iterator.next();
            WeaponSpecAPI weapon = Global.getSettings().getWeaponSpec(weaponId);
            if (weapon.getAIHints().contains(WeaponAPI.AIHints.SYSTEM)) {
                iterator.remove();
            }
        }
        factionPublicBlueprints.put(Items.WEAPON_BP, knownWeapons);

        Set<String> knownFighters = faction.getKnownFighters();
        for (Iterator<String> iterator = knownFighters.iterator(); iterator.hasNext(); ) {
            String fighterId = iterator.next();
            FighterWingSpecAPI fighter = Global.getSettings().getFighterWingSpec(fighterId);
            if (fighter.getTags().contains(Tags.WING_NO_DROP)) {
                iterator.remove();
            }
        }
        factionPublicBlueprints.put(Items.FIGHTER_BP, knownFighters);

        Set<String> knownHullMods = new HashSet<>();
//        Set<String> knownHullMods = faction.getKnownHullMods();
//        for (Iterator<String> iterator = knownHullMods.iterator(); iterator.hasNext();) {
//            String hullMod = iterator.next();
//            HullModSpecAPI hullmod = Global.getSettings().getHullModSpec(hullMod);
//            if (
//                    hullmod.getTags().contains(Tags.HULLMOD_NO_DROP) ||
//                            hullmod.getTags().contains(Tags.HULLMOD_DMOD) ||
//                            hullmod.getTags().contains(Tags.HULLMOD_NO_DROP_SALVAGE)
//            ) {
//                iterator.remove();
//            }
//        }
        factionPublicBlueprints.put(Items.MODSPEC, knownHullMods);

        return factionPublicBlueprints;
    }

    private void loadBlueprintWhitelist() {
        Logger l = Global.getLogger(this.getClass());

        whiteListedBlueprints = new HashMap<>();
        whiteListedBlueprints.put(Items.SHIP_BP, new HashSet<String>());
        whiteListedBlueprints.put(Items.WEAPON_BP, new HashSet<String>());
        whiteListedBlueprints.put(Items.FIGHTER_BP, new HashSet<String>());
        whiteListedBlueprints.put(Items.MODSPEC, new HashSet<String>());

        List<ModSpecAPI> modSpecs = Global.getSettings().getModManager().getEnabledModsCopy();
        for (ModSpecAPI mod : modSpecs) {
            try {
                JSONObject json = Global.getSettings().loadJSON("data/config/vesperon_blueprints.json", mod.getId());
                JSONObject blueprints = json.getJSONObject("availableCacheBlueprints");

                try {
                    JSONArray shipBlueprints = blueprints.getJSONArray("hullSpecIds");
                    for (int i = 0; i < shipBlueprints.length(); i++) {
                        whiteListedBlueprints.get(Items.SHIP_BP).add(shipBlueprints.getString(i));
                    }
                } catch (JSONException noHullsException) {
                    l.info("No hullSpec drop information for mod " + mod.getId());
                }

                try {
                    JSONArray weaponBlueprints = blueprints.getJSONArray("weaponSpecIds");
                    for (int i = 0; i < weaponBlueprints.length(); i++) {
                        whiteListedBlueprints.get(Items.WEAPON_BP).add(weaponBlueprints.getString(i));
                    }
                } catch (JSONException noHullsException) {
                    l.info("No weaponSpec drop information for mod " + mod.getId());
                }

                try {
                    JSONArray fighterWingIds = blueprints.getJSONArray("fighterWingIds");
                    for (int i = 0; i < fighterWingIds.length(); i++) {
                        whiteListedBlueprints.get(Items.FIGHTER_BP).add(fighterWingIds.getString(i));
                    }
                } catch (JSONException noHullsException) {
                    l.info("No fighterWing drop information for mod " + mod.getId());
                }

            } catch (IOException|RuntimeException e) {
                l.info("No blueprint drop information for mod " + mod.getId());
            } catch (JSONException e) {
                l.info("Malformed JSON in blueprint drop information for mod " + mod.getId());
            }
        }

        l.info(whiteListedBlueprints);
    }

    private boolean shouldExcludeBasedOnTags(ShipHullSpecAPI ship) {
        if (filterMode == BlueprintFilterMode.FILTER_MODE_TAGS) {
            return !ship.hasTag(Items.TAG_RARE_BP) || ship.hasTag(Tags.NO_BP_DROP) || ship.hasTag(Tags.NO_DROP);
        }
        return false;
    }

    private boolean shouldExcludeBasedOnHints(ShipHullSpecAPI ship) {
        return ship.getHints().contains(ShipTypeHints.UNBOARDABLE) ||
            ship.getHints().contains(ShipTypeHints.HIDE_IN_CODEX) ||
            ship.getHints().contains(ShipTypeHints.SHIP_WITH_MODULES) ||
            ship.getHints().contains(ShipTypeHints.STATION) ||
            ship.getHints().contains(ShipTypeHints.UNDER_PARENT);
    }

    private HashMap<String, Set<String>> getUnknownBlueprintsForFaction(FactionAPI faction) {
        HashMap<String, Set<String>> allBlueprints = new HashMap<>(allFactionBlueprints);
        HashMap<String, Set<String>> knownFactionBlueprints = getKnownBlueprintsForFaction(faction);
        for (String categoryKey : allBlueprints.keySet()) {
            Set<String> categoryBlueprints = allBlueprints.get(categoryKey);
            categoryBlueprints.removeAll(knownFactionBlueprints.get(categoryKey));
        }
        return allBlueprints;
    }

    private void generatePerson(MarketAPI market) {
        Logger l = Global.getLogger(this.getClass());
        PersonAPI agent = Global.getSector().getFaction(Factions.INDEPENDENT).createRandomPerson();
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();

        agent.setFaction(VesperonTags.VESPERON_FACTION);
        agent.addTag(VesperonTags.AGENT);
        agent.setPostId(VesperonTags.POST_AGENT);

        agent.setRankId(Ranks.CITIZEN);
        l.info("Adding person " + agent.getNameString() + " to market " + market.getName());

        market.getCommDirectory().addPerson(agent);
        market.addPerson(agent);

        ip.addPerson(agent);
        ip.getData(agent).getLocation().setMarket(market);
        ip.checkOutPerson(agent, VesperonTags.AGENT_ASSIGNED);
    }

    private VesperonFacilityCondition generateFacilityCondition(VesperonCache cache) {
        VesperonFacilityCondition level;

        float conditionPct = SequenceGenerator.get(SEQUENCE_GENERATOR_KEY).nextFloat();

        switch (cache) {
            case LOW_LEVEL_CACHE:
                conditionPct -= 0.55f;
                break;
            case MID_LEVEL_CACHE:
                conditionPct -= 0.20f;
                break;
            case HIGH_LEVEL_CACHE:
                conditionPct += 0.20f;
                break;
            case UBER_LEVEL_CACHE:
                conditionPct += 0.70f;
                break;
        }

        if (conditionPct <= 0.5f) {
            level = VesperonFacilityCondition.CONDITION_POOR;
        } else if (conditionPct <= 0.8f) {
            level = VesperonFacilityCondition.CONDITION_GOOD;
        } else {
            level = VesperonFacilityCondition.CONDITION_PRISTINE;
        }

        return level;
    }

    private void assignFacilityHazard(CustomCampaignEntityAPI facility) {
        VesperonHazard resistance;

        float dangerPct = SequenceGenerator.get(SEQUENCE_GENERATOR_KEY).nextFloat();

        if (facility.getTags().contains(VesperonFacilityCondition.CONDITION_POOR.toString())) {
            dangerPct -= 0.10f;
        } else if (facility.getTags().contains(VesperonFacilityCondition.CONDITION_GOOD.toString())) {
            dangerPct += 0.20f;
        } else if (facility.getTags().contains(VesperonFacilityCondition.CONDITION_PRISTINE.toString())) {
            dangerPct += 0.55f;
        }

        if (dangerPct <= 0.33f) {
            resistance = VesperonHazard.RESISTANCE_NONE;
        } else if (dangerPct <= 0.6f) {
            resistance = VesperonHazard.RESISTANCE_DEFENCES_LIGHT;
        } else if (dangerPct <= 0.9f) {
            resistance = VesperonHazard.RESISTANCE_DEFENCES_HEAVY;
        } else {
            resistance = VesperonHazard.RESISTANCE_DEFENCES_SUPER_HEAVY;
        }

        facility.addTag(resistance.toString());
    }

    /**
     * Gets a flat set of all unknown blueprint IDs
     *
     * @param unknownBlueprints the map of all blueprints categories
     */
    private Set<String[]> getAllNamespacedBlueprints(HashMap<String, Set<String>> unknownBlueprints) {
        Set<String[]> unknownCategoryBlueprints = new HashSet<>();
        for (String categoryKey : unknownBlueprints.keySet()) {
            for (String blueprint : unknownBlueprints.get(categoryKey)) {
                String[] bpTuple = new String[]{categoryKey, blueprint};
                unknownCategoryBlueprints.add(bpTuple);
            }
        }
        return unknownCategoryBlueprints;
    }

    /**
     * Returns a list of all terrain entities that can support a facility
     */
    private List<VesperonFacilityLocation> getSuitableTargetsForStation() {
        List<VesperonFacilityLocation> terrainCandidates = new ArrayList<>();
        List<StarSystemAPI> systems = Global.getSector().getStarSystems();

        for (StarSystemAPI system : systems) {
            if (system.isProcgen() && hasNoMarkets(system)) {
                List<CampaignTerrainAPI> terrains = system.getTerrainCopy();
                for (CampaignTerrainAPI terrain : terrains) {
                    if (
                        terrain.getPlugin().getClass() == AsteroidBeltTerrainPlugin.class
                    ) {
                        VesperonFacilityLocation location = new VesperonFacilityLocation();
                        location.system = system;
                        location.terrain = terrain;
                        terrainCandidates.add(location);
                    }
                }
            }
        }

        return terrainCandidates;
    }

    private boolean hasNoMarkets(StarSystemAPI system) {
        List<SectorEntityToken> allEntities = system.getAllEntities();
        for (SectorEntityToken entity : allEntities) {
            if (entity.getMarket() != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Alternative to using FleetFactoryV3, which doesn't appear to work for Remnants
     * (assume after procgen or without a base)
     *
     * @param facility CustomCampaignEntity to add remnant recon units to
     */
    private void assignInvestigatingFleets(CustomCampaignEntityAPI facility, int requiredFleets, int fleetPointsPerFleet) {
        Random random = new Random();
        float segments = Math.round(360f / requiredFleets);

        for (int i = 0; i < requiredFleets; i++) {
            CampaignFleetAPI remnantDefences = FleetFactoryV3.createEmptyFleet(Factions.REMNANTS, FleetTypes.PATROL_LARGE, null);

            List<ShipHullSpecAPI> knownFactionShips = getShipHullSpecsHavingAllTags(Factions.REMNANTS);
            ListMap<String> map = Global.getSettings().getHullIdToVariantListMap();
            List<ShipVariantAPI> availableVariants = getRandomVariants(random, knownFactionShips, map);

            WeightedRandomPicker<ShipVariantAPI> picker = new WeightedRandomPicker<>();
            for (ShipVariantAPI variant : availableVariants) {
                switch (variant.getHullSize()) {
                    case CRUISER:
                    case DESTROYER:
                        picker.add(variant, 2f);
                        break;
                    case CAPITAL_SHIP:
                    case FRIGATE:
                    default:
                        picker.add(variant, 1f);
                        break;
                }
            }

            for (ShipVariantAPI variant : createFleetMembers(picker, fleetPointsPerFleet)) {
                FleetMemberAPI member = remnantDefences.getFleetData().addFleetMember(variant.getHullVariantId());
                member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
            }

            remnantDefences.getFleetData().sort();
            facility.getContainingLocation().addEntity(remnantDefences);
            remnantDefences.setLocation(facility.getLocation().getX(), facility.getLocation().getY());
            remnantDefences.addAssignment(FleetAssignment.ORBIT_PASSIVE, facility, Integer.MAX_VALUE, "investigating");
            remnantDefences.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
            remnantDefences.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE, true);
            remnantDefences.getMemoryWithoutUpdate().set(VesperonTags.REMNANT_ERIPIO, true);
            RemnantSeededFleetManager.initRemnantFleetProperties(random, remnantDefences, false);
            remnantDefences.setNoFactionInName(false);
            remnantDefences.setCircularOrbit(facility, segments * i, 120, 7);
            remnantDefences.setName("Eripio");
        }

    }

    /**
     * @param tags tags in ship_data.csv to filter by; ship must have all tags
     */
    private List<ShipHullSpecAPI> getShipHullSpecsHavingAllTags(String... tags) {
        List<ShipHullSpecAPI> knownHullSpecs = Global.getSettings().getAllShipHullSpecs();
        List<ShipHullSpecAPI> knownFactionShips = new ArrayList<>();
        eachShip: for (ShipHullSpecAPI hullSpec : knownHullSpecs) {
            if (!hullSpec.getHints().contains(ShipTypeHints.STATION)) {
                for (String tag : tags) {
                    if (!hullSpec.hasTag(tag)) {
                        continue eachShip;
                    }
                }
                knownFactionShips.add(hullSpec);
            }
        }
        return knownFactionShips;
    }

    private List<ShipVariantAPI> getRandomVariants(Random random, List<ShipHullSpecAPI> knownFactionShips, ListMap<String> map) {
        List<ShipVariantAPI> availableVariants = new ArrayList<>();
        for (ShipHullSpecAPI hull : knownFactionShips) {
            List<String> variantIds = map.getList(hull.getHullId());
            if (variantIds.size() > 0) {
                String variantId = variantIds.get(random.nextInt(variantIds.size()));
                ShipVariantAPI variant = Global.getSettings().getVariant(variantId);
                availableVariants.add(variant);
            }
        }
        return availableVariants;
    }

    private Collection<ShipVariantAPI> createFleetMembers(WeightedRandomPicker<ShipVariantAPI> picker, int desiredFleetPoints) {
        int remainingFleetPoints = desiredFleetPoints;
        int originalFailures = 60, remainingFailures = 60;

        ArrayList<ShipVariantAPI> members = new ArrayList<>();

        while (remainingFleetPoints > 0 && remainingFailures > 0) {
            ShipVariantAPI shipVariant = picker.pick();
            if (shipVariant.getHullSpec().getFleetPoints() < remainingFailures) {
                remainingFleetPoints -= shipVariant.getHullSpec().getFleetPoints();
                members.add(shipVariant);
                remainingFailures = originalFailures;
            } else {
                remainingFailures--;
            }
        }
        return members;
    }

    private void assignSpecialAutomatedDefenses(CustomCampaignEntityAPI facility) {
        Logger l = Global.getLogger(this.getClass());
        Random r = new Random();
        MemoryAPI facilityMemory = facility.getMemoryWithoutUpdate();

        MarketAPI independentMarket = getMarketForFaction(Factions.INDEPENDENT);
        MarketAPI diktatMarket = getMarketForFaction(Factions.DIKTAT);
        MarketAPI tritachyonMarket = getMarketForFaction(Factions.TRITACHYON);
        MarketAPI hegemonyMarket = getMarketForFaction(Factions.HEGEMONY);

        SequenceGenerator generator = SequenceGenerator.get(SEQUENCE_GENERATOR_KEY);
        int defenseType = generator.nextInt(2);

        CampaignFleetAPI defenders = null;
        Collection<ShipVariantAPI> variants = null;
        if (facility.hasTag(VesperonHazard.RESISTANCE_DEFENCES_LIGHT.toString())) {
            l.info("Adding light defences");
            switch (defenseType) {
                case VesperonTags.RESISTANCE_TYPE_AUTOMATED:
                    defenders = FleetFactoryV3.createEmptyFleet(Factions.INDEPENDENT, FleetTypes.TASK_FORCE, null);
                    variants = new ArrayList<>(createAutomatedDefenderMembers(independentMarket, 125));
                    independentMarket.getFaction().pickShipAndAddToFleet(ShipRoles.COMBAT_CAPITAL, FactionAPI.ShipPickParams.all(), defenders, r);
                    break;
                case VesperonTags.RESISTANCE_TYPE_REMNANTS:
                    assignInvestigatingFleets(facility, 2, 60);
                    break;
            }
        } else if (facility.hasTag(VesperonHazard.RESISTANCE_DEFENCES_HEAVY.toString())) {
            l.info("Adding heavy defences");
            switch (defenseType) {
                case VesperonTags.RESISTANCE_TYPE_AUTOMATED:
                    defenders = FleetFactoryV3.createEmptyFleet(Factions.INDEPENDENT, FleetTypes.TASK_FORCE, null);
                    variants = new ArrayList<>();
                    variants.addAll(createAutomatedDefenderMembers(diktatMarket, 60));
                    variants.addAll(createAutomatedDefenderMembers(hegemonyMarket, 60));
                    variants.addAll(createAutomatedDefenderMembers(tritachyonMarket, 80));
                    hegemonyMarket.getFaction().pickShipAndAddToFleet(ShipRoles.COMBAT_CAPITAL, FactionAPI.ShipPickParams.all(), defenders, r);
                    break;
                case VesperonTags.RESISTANCE_TYPE_REMNANTS:
                    assignInvestigatingFleets(facility, 3, 60);
                    break;
            }
        } else if (facility.hasTag(VesperonHazard.RESISTANCE_DEFENCES_SUPER_HEAVY.toString())) {
            l.info("Adding superheavy defences");
            switch (defenseType) {
                case VesperonTags.RESISTANCE_TYPE_AUTOMATED:
                    defenders = FleetFactoryV3.createEmptyFleet(Factions.INDEPENDENT, FleetTypes.TASK_FORCE, null);
                    variants = new ArrayList<>();
                    variants.addAll(createAutomatedDefenderMembers(diktatMarket, 80));
                    variants.addAll(createAutomatedDefenderMembers(hegemonyMarket, 80));
                    variants.addAll(createAutomatedDefenderMembers(tritachyonMarket, 80));
                    diktatMarket.getFaction().pickShipAndAddToFleet(ShipRoles.COMBAT_CAPITAL, FactionAPI.ShipPickParams.all(), defenders, r);
                    hegemonyMarket.getFaction().pickShipAndAddToFleet(ShipRoles.COMBAT_CAPITAL, FactionAPI.ShipPickParams.all(), defenders, r);
                    break;
                case VesperonTags.RESISTANCE_TYPE_REMNANTS:
                    assignInvestigatingFleets(facility, 4, 75);
                    break;
            }

        }

        if (variants != null && !variants.isEmpty()) {
            defenders.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_PREVENT_DISENGAGE, true);
            augmentDefenseFleet(defenders, variants);
            defenders.getFleetData().sort();
            facilityMemory.set("$salvageLeaveText", "Leave");
            facilityMemory.set("$defenderFleet", defenders);
            facilityMemory.set("$hasDefenders", true);
        } else {
            facilityMemory.set("$hasDefenders", false);
        }
    }

    private void augmentDefenseFleet(CampaignFleetAPI defenders, Collection<ShipVariantAPI> variants) {
        Logger l = Global.getLogger(this.getClass());
        for (ShipVariantAPI variant : variants) {
            l.info("Selected variant: " + variant.getHullVariantId());
            defenders.getFleetData().addFleetMember(variant.clone().getHullVariantId());
        }

        FleetDataAPI newFleetData = defenders.getFleetData();
        defenders.setName("Automated Defenses");
        defenders.clearAbilities();
        l.info("Adding defenders");

        Collection<FleetMemberAPI> newDefenderMembers = newFleetData.getMembersInPriorityOrder();

        for (FleetMemberAPI member : newDefenderMembers) {

            // Add automation centres and targeting clusters
            ShipVariantAPI temporaryVariant = member.getVariant().clone();
            temporaryVariant.setOriginalVariant(null);
            temporaryVariant.setSource(VariantSource.REFIT);
            temporaryVariant.addPermaMod(VesperonHullMods.AUTOMATION_CENTRE);

            if (temporaryVariant.hasHullMod(HullMods.INTEGRATED_TARGETING_UNIT)) {
                temporaryVariant.removeMod(HullMods.INTEGRATED_TARGETING_UNIT);
            }
            if (temporaryVariant.hasHullMod(HullMods.DEDICATED_TARGETING_CORE)) {
                temporaryVariant.removeMod(HullMods.DEDICATED_TARGETING_CORE);
            }

            // TODO - re-enable this, if I can work out how to deploy capitals first
//            if (member.isCapital()) {
//                temporaryVariant.addPermaMod(VesperonHullMods.TARGETING_CLUSTER_MASTER);
//            } else {
//                temporaryVariant.addPermaMod(VesperonHullMods.TARGETING_CLUSTER_SLAVE);
//            }

            temporaryVariant.addPermaMod(VesperonHullMods.TARGETING_CLUSTER_NODE);

            temporaryVariant.setVariantDisplayName("Automated Defense");
            member.setVariant(temporaryVariant, false, true);
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());

            l.info("Defender hull name: " + member.getHullSpec().getHullName());
        }
    }

    private Collection<ShipVariantAPI> createAutomatedDefenderMembers(MarketAPI market, int desiredFleetPoints) {
        Logger l = Global.getLogger(this.getClass());
        int remainingFleetPoints = desiredFleetPoints;
        int remainingFailures = 60;
        ArrayList<ShipVariantAPI> members = new ArrayList<>();

        Set<String> cruisers = (market.getFaction().getVariantsForRole(ShipRoles.COMBAT_LARGE));
        Set<String> destroyers = (market.getFaction().getVariantsForRole(ShipRoles.COMBAT_MEDIUM));
        Set<String> frigates = (market.getFaction().getVariantsForRole(ShipRoles.COMBAT_SMALL));

        WeightedRandomPicker<ShipVariantAPI> picker = new WeightedRandomPicker<>();

        for (String hullId : cruisers) {
            ShipVariantAPI hull = Global.getSettings().getVariant(hullId).clone();
            l.info("Adding hullID to picker: " + hullId);
            picker.add(hull, 3f);
        }
        for (String hullId : destroyers) {
            ShipVariantAPI hull = Global.getSettings().getVariant(hullId).clone();
            l.info("Adding hullID to picker: " + hullId);
            picker.add(hull, 2f);
        }
        for (String hullId : frigates) {
            ShipVariantAPI hull = Global.getSettings().getVariant(hullId).clone();
            l.info("Adding hullID to picker: " + hullId);
            picker.add(hull, 1f);
        }

        while (remainingFleetPoints > 0 && remainingFailures > 0) {
            ShipVariantAPI variant = picker.pick();
            l.info(variant.getHullVariantId());
            if (variant.getHullSpec().getFleetPoints() < remainingFailures) {
                remainingFleetPoints -= variant.getHullSpec().getFleetPoints();
                remainingFailures = 60;
                members.add(variant);
            } else {
                remainingFailures--;
            }
        }
        return members;
    }



    private MarketAPI getMarketForFaction(String factionId) {
        Logger l = Global.getLogger(this.getClass());
        MarketAPI chosenMarket = null;
        List<MarketAPI> marketsCopy = Global.getSector().getEconomy().getMarketsCopy();
        for (MarketAPI market : marketsCopy) {
            if (market.getFactionId().equals(factionId)) {
                l.info("Choosing market for fleet gen params: " + market.getName());
                chosenMarket = market;
                break;
            }
        }
        return chosenMarket;

    }


}
