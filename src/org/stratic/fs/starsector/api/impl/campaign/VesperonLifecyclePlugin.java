package org.stratic.fs.starsector.api.impl.campaign;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import org.stratic.fs.starsector.api.impl.campaign.ids.VesperonTags;
import org.stratic.fs.starsector.api.impl.campaign.intel.bar.events.VesperonMembershipBarEventCreator;

public class VesperonLifecyclePlugin extends BaseModPlugin {
    @Override
    public void onGameLoad(boolean newGame) {
        // determine if previously enabled
        MemoryAPI m = Global.getSector().getMemoryWithoutUpdate();
        boolean enabled = m.getBoolean(VesperonTags.MOD_ENABLED);
        if (!enabled) {
            Global.getLogger(this.getClass()).info("Enabling Vesperon mod");
            m.set(VesperonTags.MOD_ENABLED, true);
        }

        BarEventManager bar = BarEventManager.getInstance();
        if (!bar.hasEventCreator(VesperonMembershipBarEventCreator.class)) {
            bar.addEventCreator(new VesperonMembershipBarEventCreator());
        }


    }
}
