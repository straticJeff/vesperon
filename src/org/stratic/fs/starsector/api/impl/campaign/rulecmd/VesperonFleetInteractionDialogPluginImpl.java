package org.stratic.fs.starsector.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;

import java.util.List;

public class VesperonFleetInteractionDialogPluginImpl extends FleetInteractionDialogPluginImpl {

    public VesperonFleetInteractionDialogPluginImpl(FIDConfig params) {
        super(params);
        context = new VesperonFleetEncounterContext();
    }

    private class VesperonFleetEncounterContext extends FleetEncounterContext {
        @Override
        public List<FleetMemberAPI> getRecoverableShips(BattleAPI battle, CampaignFleetAPI winningFleet, CampaignFleetAPI otherFleet) {
            List<FleetMemberAPI> allRecoverable = super.getRecoverableShips(battle, winningFleet, otherFleet);
//            List<FleetMemberAPI> exclusions = new ArrayList<>();
//            Global.getLogger("Winning fleet faction:" + winningFleet.getFaction().getId());
            if (!winningFleet.isPlayerFleet()) {
//                Global.getLogger(this.getClass()).info("Removing enemy ships from recoverables");
                DataForEncounterSide winnerData = getDataFor(winningFleet);
                for (FleetMemberData memberData : winnerData.getEnemyCasualties()) {

//                    Global.getLogger(this.getClass()).info("Excluding: " + memberData.getMember().getHullId());
                    allRecoverable.remove(memberData.getMember());
                }
            }
//            Global.getLogger(this.getClass()).info("Remaining hulls: ");
            for (FleetMemberAPI member : allRecoverable) {
//                Global.getLogger(this.getClass()).info(member.getHullId());
            }
            return allRecoverable;
        }
    }

    @Override
    protected void winningPath() {
        super.winningPath();
    }

}
