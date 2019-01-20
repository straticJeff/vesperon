package org.stratic.fs.starsector.api.impl.campaign.intel.bar.events;

import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

public class VesperonMembershipBarEventCreator extends BaseBarEventCreator {

    static float MEMBERSHIP_BAR_EVENT_TIMEOUT = 6f;

    public PortsideBarEvent createBarEvent() {
        return new VesperonMembershipBarEvent();
    }

    @Override
    public float getBarEventTimeoutDuration() {
        return Integer.MAX_VALUE * 1f;
    }
}