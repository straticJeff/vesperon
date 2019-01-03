package org.stratic.fs.starsector.api.impl.campaign.ids;

public class VesperonTags {

    // Factions
    public static final String VESPERON_FACTION = "vesperon";

    // Memory API keys
    public static final String MOD_ENABLED = "$vesperon_enabled";
    public static final String REP_MARKET = "$vesperonRepMarket";
    public static final String CURRENT_CACHE_COUNT = "$currentVesperonCacheCount";
    public static final String CACHE_OPTION = "$vesperonCacheOption";

    public static final String AVAILABILITY_LEVEL_LOW = "$lowLevelCacheAvailable";
    public static final String AVAILABILITY_LEVEL_MID = "$midLevelCacheAvailable";
    public static final String AVAILABILITY_LEVEL_HIGH = "$highLevelCacheAvailable";
    public static final String AVAILABILITY_LEVEL_UBER = "$uberLevelCacheAvailable";

    public static final String PRICE_LOW = "$vesperonLowLevelCachePrice";
    public static final String PRICE_MID = "$vesperonMidLevelCachePrice";
    public static final String PRICE_HIGH = "$vesperonHighLevelCachePrice";
    public static final String PRICE_UBER = "$vesperonUberLevelCachePrice";

    // Rule tags
    public static final String BASE = "vesperon";
    public static final String AGENT = "vesperon_agent";

    // Facility grades
    public static final String FACILITY_BASE = "vesperon_facility";
    public static final String FACILITY_POOR = "vesperon_facility_t1";
    public static final String FACILITY_GOOD = "vesperon_facility_t2";
    public static final String FACILITY_PRISTINE = "vesperon_facility_t3";

    // Agents
    public static final String AGENT_ASSIGNED = "vesperon_agent_assigned";
    public static final String POST_AGENT = "vesperonRep";

    public static final String CACHE_OPTION_LOW = "LOW";
    public static final String CACHE_OPTION_MID = "MID";
    public static final String CACHE_OPTION_HIGH = "HIGH";
    public static final String CACHE_OPTION_UBER = "UBER";

    // Location constants
    public static final int MIN_ORBITAL_PERIOD = 15;

    // Remnant memflags
    public static final String REMNANT_ERIPIO = "$vesperonRemnantEripio";

    // Resistance types
    public static final int RESISTANCE_TYPE_AUTOMATED = 0;
    public static final int RESISTANCE_TYPE_REMNANTS = 1;


    // Facility conditions
    public enum VesperonFacilityCondition {
        CONDITION_PRISTINE,
        CONDITION_GOOD,
        CONDITION_POOR,
    }

    // Facility danger levels
    public enum VesperonHazard {
        RESISTANCE_NONE,
        RESISTANCE_TRAPS,
        RESISTANCE_DEFENCES_LIGHT,
        RESISTANCE_DEFENCES_HEAVY,
        RESISTANCE_DEFENCES_SUPER_HEAVY,
    }

    // AvailableCache grades
    public enum VesperonCache {
        LOW_LEVEL_CACHE(125000, 190000),
        MID_LEVEL_CACHE(225000, 300000),
        HIGH_LEVEL_CACHE(375000, 550000),
        UBER_LEVEL_CACHE(1100000, 1600000);

        public final double minValue;
        public final double maxValue;

        VesperonCache(double minValue, double maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

    }


}
