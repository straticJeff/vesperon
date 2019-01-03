package org.stratic.fs.starsector.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

public class AutomationCentre extends BaseHullMod {

    public static final float SHIELD_DAMAGE_REDUCTION_BONUS_MULT = 0.1f;
    public static final float SPEED_BONUS_MULT = 0.3f;
    public static final float PEAK_CR_MULT = 0.5f;

    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getShieldDamageTakenMult().modifyMult(id, 1f - SHIELD_DAMAGE_REDUCTION_BONUS_MULT);
        stats.getMaxSpeed().modifyMult(id, 1f + SPEED_BONUS_MULT);
        stats.getPeakCRDuration().modifyMult(id, 1f - PEAK_CR_MULT);
        stats.getMinCrewMod().modifyMult(id, 0f);
        stats.getMaxCrewMod().modifyMult(id, 0f);
    }

    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int) (SHIELD_DAMAGE_REDUCTION_BONUS_MULT * 100) + "%";
        if (index == 1) return "" + (int) (SPEED_BONUS_MULT * 100) + "%";
        if (index == 2) return "" + (int) (PEAK_CR_MULT * 100) + "%";
        return null;
    }
}
