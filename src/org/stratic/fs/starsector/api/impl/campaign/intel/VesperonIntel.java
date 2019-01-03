package org.stratic.fs.starsector.api.impl.campaign.intel;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.misc.BreadcrumbIntel;
import com.fs.starfarer.api.ui.SectorMapAPI;

import java.util.HashSet;
import java.util.Set;

public class VesperonIntel extends BreadcrumbIntel {

    public VesperonIntel(SectorEntityToken foundAt, SectorEntityToken target) {
        super(foundAt, target);
        this.setTitle("Vesperon dossier: camouflaged orbital facility");
        this.setText(
            "The Vesperon dossier you purchased points to an orbital facility in the " + foundAt.getFullName() + " " +
                "system."
        );
    }

    @Override
    public String getIcon() {
        return super.getIcon();
    }

    @Override
    public boolean shouldRemoveIntel() {
        return super.shouldRemoveIntel();
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_EXPLORATION);
        return tags;
    }
}
