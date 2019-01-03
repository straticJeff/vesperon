package org.stratic.fs.starsector.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.stratic.fs.starsector.api.impl.campaign.ids.VesperonHullMods;

import java.util.List;
import java.util.Random;

public class TargetingClusterNode extends BaseHullMod {
    public static final float ENHANCEMENT_BONUS_FRIGATE = 2f;
    public static final float ENHANCEMENT_BONUS_DESTROYER = 3f;
    public static final float ENHANCEMENT_BONUS_CRUISER = 5f;
    public static final float ENHANCEMENT_BONUS_CAPITAL = 10f;

    public static final float RANGE_BONUS_CAP = 175f;


    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        int nodes = 0;
        float bonus = 0f;

        List<FleetMemberAPI> alliedFleet = Global.getCombatEngine().getFleetManager(ship.getOwner()).getDeployedCopy();
        for (FleetMemberAPI member : alliedFleet) {
            ShipAPI otherShip = Global.getCombatEngine().getFleetManager(ship.getOwner()).getShipFor(member);
            if (otherShip.isAlive() && !otherShip.getId().equals(ship.getId())) {
                if (
                    otherShip.getVariant().hasHullMod(VesperonHullMods.TARGETING_CLUSTER_SLAVE) ||
                    otherShip.getVariant().hasHullMod(VesperonHullMods.TARGETING_CLUSTER_NODE)
                ) {
                    nodes++;
                }
            }
        }

        float nodeBonus = 0f;
        if (ship.isFrigate()) {
            nodeBonus = TargetingClusterNode.ENHANCEMENT_BONUS_FRIGATE;
        } else if (ship.isDestroyer()) {
            nodeBonus = TargetingClusterNode.ENHANCEMENT_BONUS_DESTROYER;
        } else if (ship.isCruiser()) {
            nodeBonus = TargetingClusterNode.ENHANCEMENT_BONUS_CRUISER;
        } else if (ship.isCapital()) {
            nodeBonus = TargetingClusterNode.ENHANCEMENT_BONUS_CAPITAL;
        }
        bonus += Math.min((nodeBonus * nodes), TargetingClusterMaster.RANGE_BONUS_CAP);

        ship.getMutableStats().getBallisticWeaponRangeBonus().modifyPercent(VesperonHullMods.TARGETING_CLUSTER_SLAVE, bonus);
        ship.getMutableStats().getEnergyWeaponRangeBonus().modifyPercent(VesperonHullMods.TARGETING_CLUSTER_SLAVE, bonus);
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        switch (index) {
            case 0:
                return "" + (int)(ENHANCEMENT_BONUS_FRIGATE) + "%";
            case 1:
                return "" + (int)(ENHANCEMENT_BONUS_DESTROYER) + "%";
            case 2:
                return "" + (int)(ENHANCEMENT_BONUS_CRUISER) + "%";
            case 3:
                return "" + (int)(ENHANCEMENT_BONUS_CAPITAL) + "%";
            case 4:
                return "" + (int)(RANGE_BONUS_CAP) + "%";
        }
        return null;
    }
}
