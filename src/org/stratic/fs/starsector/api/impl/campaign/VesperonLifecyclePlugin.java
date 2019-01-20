package org.stratic.fs.starsector.api.impl.campaign;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import org.stratic.fs.starsector.api.impl.campaign.ids.VesperonTags;
import org.stratic.fs.starsector.api.impl.campaign.intel.VesperonIntelManager;
import org.stratic.fs.starsector.api.impl.campaign.intel.bar.events.VesperonMembershipBarEventCreator;

@SuppressWarnings("unused")
public class VesperonLifecyclePlugin extends BaseModPlugin {

    private VesperonIntelManager manager;

    @Override
    public void onGameLoad(boolean newGame) {
        // determine if previously enabled
        MemoryAPI m = Global.getSector().getMemoryWithoutUpdate();
        boolean enabled = m.getBoolean(VesperonTags.MOD_ENABLED);
        if (!enabled) {
            Global.getLogger(this.getClass()).info("Enabling Vesperon mod for first time");
            m.set(VesperonTags.MOD_ENABLED, true);
        }

        BarEventManager bar = BarEventManager.getInstance();
        if (!bar.hasEventCreator(VesperonMembershipBarEventCreator.class)) {
            bar.addEventCreator(new VesperonMembershipBarEventCreator());
        }

        // initialise blueprint manager with configured filter mode
        VesperonIntelManager.getInstance();
    }



    @Override
    // make sure any active intel manager is destroyed so it's state doesn't get into the save file...
    public void beforeGameSave() {
        MemoryAPI m = Global.getSector().getMemoryWithoutUpdate();
        if (m.contains(VesperonTags.KEY_MANAGER_INSTANCE)) {
            this.manager = (VesperonIntelManager)m.get(VesperonTags.KEY_MANAGER_INSTANCE);
            m.unset(VesperonTags.KEY_MANAGER_INSTANCE);
        }
    }

    @Override
    // ...and reload it afterwards
    public void afterGameSave() {
        MemoryAPI m = Global.getSector().getMemoryWithoutUpdate();
        m.set(VesperonTags.KEY_MANAGER_INSTANCE, this.manager);
    }

    @Override
    public void onApplicationLoad() throws Exception {
        super.onApplicationLoad();
    }
}
