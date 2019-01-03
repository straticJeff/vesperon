package org.stratic.fs.starsector.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc;
import org.stratic.fs.starsector.api.impl.campaign.ids.VesperonTags;
import org.stratic.fs.starsector.api.impl.campaign.ids.VesperonTags.VesperonCache;
import org.stratic.fs.starsector.api.impl.campaign.intel.VesperonIntelManager;
import org.stratic.fs.starsector.util.SequenceGenerator;

import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

public class VesperonCaches extends BaseCommandPlugin {

    private static final float EXPIRATION_TIME = 10f;


    public enum OptionId {
        LOW_LEVEL_CACHE,
        MID_LEVEL_CACHE,
        HIGH_LEVEL_CACHE,
        UBER_LEVEL_CACHE
    }

    public class AvailableCache {
        String priceKey;
        VesperonCache cache;
        boolean available;

        AvailableCache(String priceKey, VesperonCache cache, boolean available) {
            this.priceKey = priceKey;
            this.cache = cache;
            this.available = available;
        }
    }


    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        String command = params.get(0).getString(memoryMap);
        if (command == null) {
            return false;
        }
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();

        switch (command) {
            case "regen":
                spawnAvailableCaches(memory);
                break;
            case "options":
                getDialogueOptions(dialog, memory);
                break;
            case "create":
                createCache(memory);
                break;
        }
        return true;
    }

    private void createCache(MemoryAPI memory) {
        String cacheOption = Global.getSector().getMemoryWithoutUpdate().getString(VesperonTags.CACHE_OPTION);
        VesperonCache v = null;
        String memoryKey = null;
        long cacheCount = memory.getLong(VesperonTags.CURRENT_CACHE_COUNT);
        switch (cacheOption) {
            case VesperonTags.CACHE_OPTION_LOW:
                v = VesperonCache.LOW_LEVEL_CACHE;
                memoryKey = VesperonTags.AVAILABILITY_LEVEL_LOW;
                break;
            case VesperonTags.CACHE_OPTION_MID:
                v = VesperonCache.MID_LEVEL_CACHE;
                memoryKey = VesperonTags.AVAILABILITY_LEVEL_MID;
                break;
            case VesperonTags.CACHE_OPTION_HIGH:
                v = VesperonCache.HIGH_LEVEL_CACHE;
                memoryKey = VesperonTags.AVAILABILITY_LEVEL_HIGH;
                break;
            case VesperonTags.CACHE_OPTION_UBER:
                v = VesperonCache.UBER_LEVEL_CACHE;
                memoryKey = VesperonTags.AVAILABILITY_LEVEL_UBER;
                break;
        }

        removeCacheOption(memory, memoryKey, cacheCount);

        VesperonIntelManager intelManager = new VesperonIntelManager();
        intelManager.createVesperonMission(v);
    }

    private void removeCacheOption(MemoryAPI memory, String memoryKey, long cacheCount) {
        memory.set(memoryKey, false);
        memory.set(VesperonTags.CURRENT_CACHE_COUNT, cacheCount - 1, memory.getExpire(VesperonTags.CURRENT_CACHE_COUNT));
    }

    private void getDialogueOptions(InteractionDialogAPI dialog, MemoryAPI memory) {
        boolean lowCacheAvailable;
        boolean midCacheAvailable;
        boolean highCacheAvailable;
        boolean uberCacheAvailable;
        lowCacheAvailable = memory.getBoolean(VesperonTags.AVAILABILITY_LEVEL_LOW);
        midCacheAvailable = memory.getBoolean(VesperonTags.AVAILABILITY_LEVEL_MID);
        highCacheAvailable = memory.getBoolean(VesperonTags.AVAILABILITY_LEVEL_HIGH);
        uberCacheAvailable = memory.getBoolean(VesperonTags.AVAILABILITY_LEVEL_UBER);
        dialog.getOptionPanel().clearOptions();

        if (lowCacheAvailable) {
            dialog.getOptionPanel().addOption("Ask about the level 1 cache", OptionId.LOW_LEVEL_CACHE.toString());
        }
        if (midCacheAvailable) {
            dialog.getOptionPanel().addOption("Ask about the level 2 cache", OptionId.MID_LEVEL_CACHE.toString());
        }
        if (highCacheAvailable) {
            dialog.getOptionPanel().addOption("Ask about the level 3 cache", OptionId.HIGH_LEVEL_CACHE.toString());
        }
        if (uberCacheAvailable) {
            dialog.getOptionPanel().addOption("Ask about the high grade cache", OptionId.UBER_LEVEL_CACHE.toString());
        }
        dialog.getOptionPanel().addOption("Cut the comm link", "vesperonAgentLeave", null);
    }

    private void spawnAvailableCaches(MemoryAPI memory) {
        boolean lowCacheAvailable;
        boolean midCacheAvailable;
        boolean highCacheAvailable;
        boolean uberCacheAvailable;
        int count = 0;

        Global.getLogger(this.getClass()).info("Cache count expiration: " + memory.getExpire(VesperonTags.CURRENT_CACHE_COUNT));
        Global.getLogger(this.getClass()).info("Does key exist: " + memory.contains(VesperonTags.CURRENT_CACHE_COUNT));

        if (!memory.contains(VesperonTags.CURRENT_CACHE_COUNT) || memory.getExpire(VesperonTags.CURRENT_CACHE_COUNT) < 0) {
            Global.getLogger(this.getClass()).info("Regenerating caches");
            lowCacheAvailable = SequenceGenerator.get("lowLevelCacheGenerator").nextInt(2) == 0;
            midCacheAvailable = SequenceGenerator.get("midLevelCacheGenerator").nextInt(4) == 0;
            highCacheAvailable = SequenceGenerator.get("highLevelCacheGenerator").nextInt(6) == 0;
            uberCacheAvailable = SequenceGenerator.get("uberLevelCacheGenerator").nextInt(9) == 0;

            memory.set(VesperonTags.AVAILABILITY_LEVEL_LOW, lowCacheAvailable);
            memory.set(VesperonTags.AVAILABILITY_LEVEL_MID, midCacheAvailable);
            memory.set(VesperonTags.AVAILABILITY_LEVEL_HIGH, highCacheAvailable);
            memory.set(VesperonTags.AVAILABILITY_LEVEL_UBER, uberCacheAvailable);

            AvailableCache[] caches = {
                new AvailableCache(VesperonTags.PRICE_LOW, VesperonCache.LOW_LEVEL_CACHE, lowCacheAvailable),
                new AvailableCache(VesperonTags.PRICE_MID, VesperonCache.MID_LEVEL_CACHE, midCacheAvailable),
                new AvailableCache(VesperonTags.PRICE_HIGH, VesperonCache.HIGH_LEVEL_CACHE, highCacheAvailable),
                new AvailableCache(VesperonTags.PRICE_UBER, VesperonCache.UBER_LEVEL_CACHE, uberCacheAvailable),
            };

            for (AvailableCache cache : caches) {
                SequenceGenerator generator = SequenceGenerator.get(cache.priceKey);
                int basePrice = generator.nextInt((int)cache.cache.maxValue) + (int)cache.cache.minValue;
                int price = (basePrice * (generator.nextInt(2) + 1));
                String priceStr = NumberFormat.getInstance().format(price);
                memory.set(
                    cache.priceKey,
                    price,
                    EXPIRATION_TIME
                );
                memory.set(
                    cache.priceKey + "Str",
                    priceStr,
                    EXPIRATION_TIME
                );
                if (cache.available) {
                    count++;
                }
            }
            memory.set(VesperonTags.CURRENT_CACHE_COUNT, count, EXPIRATION_TIME);
        } else {
            Global.getLogger(this.getClass()).info("Existing count expiration: " + memory.getExpire(VesperonTags.CURRENT_CACHE_COUNT));
        }
    }
}