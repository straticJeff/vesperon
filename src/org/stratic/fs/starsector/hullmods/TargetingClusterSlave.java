package org.stratic.fs.starsector.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.stratic.fs.starsector.api.impl.campaign.ids.VesperonHullMods;

import java.util.List;
import java.util.Random;

public class TargetingClusterSlave extends BaseHullMod {
    public static final float ENHANCEMENT_BONUS_FRIGATE_MULT = 2f;
    public static final float ENHANCEMENT_BONUS_DESTROYER_MULT = 4f;
    public static final float ENHANCEMENT_BONUS_CRUISER_MULT = 6f;
    public static final float ENHANCEMENT_BONUS_CAPITAL_MULT = 10f;


    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        boolean masterPresent = false;
        int slaves = 0;
        float bonus = 0f;

        List<FleetMemberAPI> alliedFleet = Global.getCombatEngine().getFleetManager(ship.getOwner()).getDeployedCopy();
        for (FleetMemberAPI member : alliedFleet) {
            ShipAPI otherShip = Global.getCombatEngine().getFleetManager(ship.getOwner()).getShipFor(member);
            if (otherShip.isAlive() && !otherShip.getId().equals(ship.getId())) {
                if (otherShip.getVariant().hasHullMod(VesperonHullMods.TARGETING_CLUSTER_MASTER)) {
                    masterPresent = true;
                }
                if (otherShip.getVariant().hasHullMod(VesperonHullMods.TARGETING_CLUSTER_SLAVE)) {
                    slaves++;
                }
            }
        }

        if (masterPresent) {
            float masterBonus = 0f;
            float slaveBonus = 0f;
            if (ship.isFrigate()) {
                masterBonus = TargetingClusterMaster.ENHANCEMENT_BONUS_FRIGATE_MULT;
                slaveBonus = TargetingClusterSlave.ENHANCEMENT_BONUS_FRIGATE_MULT;
            } else if (ship.isDestroyer()) {
                masterBonus = TargetingClusterMaster.ENHANCEMENT_BONUS_DESTROYER_MULT;
                slaveBonus = TargetingClusterSlave.ENHANCEMENT_BONUS_DESTROYER_MULT;
            } else if (ship.isCruiser()) {
                masterBonus = TargetingClusterMaster.ENHANCEMENT_BONUS_CRUISER_MULT;
                slaveBonus = TargetingClusterSlave.ENHANCEMENT_BONUS_CRUISER_MULT;
            } else if (ship.isCapital()) {
                masterBonus = TargetingClusterMaster.ENHANCEMENT_BONUS_CAPITAL_MULT;
                slaveBonus = TargetingClusterSlave.ENHANCEMENT_BONUS_CAPITAL_MULT;
            }
            bonus += Math.min(masterBonus + (slaveBonus * slaves), TargetingClusterMaster.RANGE_BONUS_CAP);


            ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent(VesperonHullMods.TARGETING_CLUSTER_SLAVE, bonus);
            ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent(VesperonHullMods.TARGETING_CLUSTER_SLAVE, bonus);
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        switch (index) {
            case 0:
                return "" + (int)(ENHANCEMENT_BONUS_FRIGATE_MULT) + "%";
            case 1:
                return "" + (int)(ENHANCEMENT_BONUS_DESTROYER_MULT) + "%";
            case 2:
                return "" + (int)(ENHANCEMENT_BONUS_CRUISER_MULT) + "%";
            case 3:
                return "" + (int)(ENHANCEMENT_BONUS_CAPITAL_MULT) + "%";
        }
        return null;
    }
}
