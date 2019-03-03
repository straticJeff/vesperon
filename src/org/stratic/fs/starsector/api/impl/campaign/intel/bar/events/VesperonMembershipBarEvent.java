package org.stratic.fs.starsector.api.impl.campaign.intel.bar.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.stratic.fs.starsector.api.impl.campaign.ids.VesperonTags;
import org.stratic.fs.starsector.api.impl.campaign.intel.VesperonIntelManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.stratic.fs.starsector.api.impl.campaign.intel.bar.events.VesperonMembershipBarEventCreatorV2.MEMBERSHIP_BAR_EVENT_TIMEOUT;

public class VesperonMembershipBarEvent extends BaseBarEventWithPerson {

    public enum OptionId {
        INIT,
        INTRO_LOW_REP,
        INTRO_1,
        INTRO_2,
        INTRO_3,
        INTRO_4,
        PAY_FEE,
        PAY_LATER,
        LEAVE,
        FIND_AGENTS,
        FIND_BASE,
        ANY_ADVICE,
        LEAVE_DONE,
    }

    private final static double MEMBERSHIP_FEE = 1000000;
    private final static double REP_MARKET_COUNT = 2;

    private VesperonIntelManager vesperonIntelManager;

    VesperonMembershipBarEvent() {
        super();
        vesperonIntelManager = VesperonIntelManager.getInstance();
    }

    public boolean shouldShowAtMarket(MarketAPI currentMarket) {
        if (Global.getSector().getMemoryWithoutUpdate().contains(VesperonTags.AGENTS_ADDED)) {
            return false;
        }
        if (Global.getSector().getMemoryWithoutUpdate().get(VesperonTags.REP_MARKET_IDS) == null) {
            setRepMarkets();
        }
        @SuppressWarnings("unchecked")
        List<String> marketIds = (List<String>)Global.getSector().getMemoryWithoutUpdate().get(VesperonTags.REP_MARKET_IDS);
        @SuppressWarnings("unchecked")
        List<PersonAPI> people = (List<PersonAPI>)Global.getSector().getMemoryWithoutUpdate().get(VesperonTags.REP_PEOPLE);

        for (String marketId : marketIds) {
            Global.getLogger(this.getClass()).info("Vesperon rep currently located at market " + Global.getSector().getEconomy().getMarket(marketId).getName());
        }

        vesperonIntelManager = VesperonIntelManager.getInstance();

        for (int i = 0; i < marketIds.size(); i++) {
            if (marketIds.get(i).equals(currentMarket.getId())) {
                this.person = people.get(i);
                return true;
            }
        }
        return false;
    }

    private void setRepMarkets() {
        MemoryAPI memory = Global.getSector().getMemoryWithoutUpdate();

        WeightedRandomPicker<MarketAPI> picker = new WeightedRandomPicker<>(random);
        for (MarketAPI currentMarket : Global.getSector().getEconomy().getMarketsCopy()) {
            if (currentMarket.isPlayerOwned()) {
                continue;
            }
            if (currentMarket.isHidden()) {
                continue;
            }
            if (!currentMarket.getFactionId().equals(Factions.INDEPENDENT)) {
                continue;
            }

            float w = currentMarket.getSize();
            picker.add(currentMarket, w);
        }

        List<String> marketIds = new ArrayList<>();
        List<PersonAPI> people = new ArrayList<>();

        for (int i = 0; i < REP_MARKET_COUNT; i++) {
            MarketAPI pickedMarket = picker.pickAndRemove();
            marketIds.add(pickedMarket.getId());

            PersonAPI person = createPerson();
            people.add(person);
        }
        memory.set(VesperonTags.REP_MARKET_IDS, marketIds, MEMBERSHIP_BAR_EVENT_TIMEOUT);
        memory.set(VesperonTags.REP_PEOPLE, people, MEMBERSHIP_BAR_EVENT_TIMEOUT);
    }

    @Override
    protected PersonAPI createPerson() {
        PersonAPI newPerson = Global.getSector().getFaction(getPersonFaction()).createRandomPerson(getPersonGender(), random);
        if (newPerson.getGender() == Gender.MALE) {
            newPerson.setPortraitSprite(Global.getSettings().getSpriteName("intel", "vesperon_male"));
        } else {
            newPerson.setPortraitSprite(Global.getSettings().getSpriteName("intel", "vesperon_female"));
        }
        newPerson.setRankId(Ranks.AGENT);
        newPerson.setPostId(Ranks.POST_AGENT);

        return newPerson;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        super.init(dialog);

        done = false;

        dialog.getVisualPanel().showPersonInfo(person, true);

        optionSelected(null, OptionId.INIT);
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog) {
        super.addPromptAndOption(dialog);

        TextPanelAPI text = dialog.getTextPanel();
        text.addPara(
                "An impeccably suited " + getManOrWoman() + " is drinking something expensive in the more " +
                        "shadowed area of the bar. On the table in front of " + getHimOrHer() + " is a high-tech case " +
                        "emblazoned with a strange logo. "
        );

        Color c = Misc.getStoryOptionColor();

        dialog.getOptionPanel().addOption(
                "Ask the " + getManOrWoman() + " what " + getHeOrShe() + "'s doing here",
                this,
                c,
                null
        );
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        if (!(optionData instanceof OptionId)) {
            return;
        }
        OptionId option = (OptionId) optionData;

        OptionPanelAPI options = dialog.getOptionPanel();
        TextPanelAPI text = dialog.getTextPanel();
        options.clearOptions();

        RepLevel independentReputation = Global.getSector().getPlayerFaction().getRelationshipLevel(
                Global.getSector().getFaction(Factions.INDEPENDENT)
        );

        switch (option) {
            case INIT:
                text.addPara(
                        "\"Ahh, hello! Have you heard of the Vesperon Combine?\", " + getHeOrShe() + " asks " +
                                "warmly. The name of the organization is vaguely memorable."
                );

                OptionId next;
                if (independentReputation.isAtWorst(RepLevel.FAVORABLE)) {
                    next = OptionId.INTRO_1;
                } else {
                    next = OptionId.INTRO_LOW_REP;
                }

                options.addOption("Tell " + getHimOrHer() + " you haven't", next);
                options.addOption("Awkwardly look at your TriChron watch, make your excuses, and leave", OptionId.LEAVE);
                break;
            case INTRO_LOW_REP:
                text.addPara(
                        "\"Let's just say we're an organisation on the lookout for friends to the non-aligned worlds,\" " +
                                getHeOrShe() + " replies cryptically. \"We've been looking for people like yourself who might fit " +
                                "into that picture. We'll speak again when that time comes.\""
                );
                text.addPara(
                        "You have a feeling increasing your standing with the Independents will encourage " +
                        getHimOrHer() + " to be more forthcoming."
                );
                options.addOption("Leave", OptionId.LEAVE);
                break;
            case INTRO_1:
                text.addPara(
                        "\"Well, friend, let's remedy that. We're an organisation that would like to see " +
                                "the destiny of the Persean Sector in the hands of its people, rather than the corrupt " +
                                "mess of dictators, executives and preachers that control it as of now\", he explains."
                );
                text.addPara(
                        "\"To that end, we're looking for enterprising spacefarers like yourself who we can work " +
                                "with to make this a reality.\" " + Misc.ucFirst(getHeOrShe()) + " pushes a " +
                                "button on " + getHisOrHer() + " briefcase. A small multi-factor bioscanner quickly validates its owner's " +
                                "identity, and with a beep, the case opens by itself to reveal a robust integrated " +
                                "terminal, which immediately powers itself up with a subtle glow. The rep motions for " +
                                "you to take a look."
                );

                options.addOption("Take a look at the terminal", OptionId.INTRO_2);
                break;
            case INTRO_2:
                text.addPara(
                        "The representative explains that the Vesperon Combine has access to intelligence " +
                                "on a number of hidden sites in the outermost reaches of the sector. " +
                                "A number of deliberately enlarged areas ping on the screen, showing the approximate " +
                                "locations of these sites. They all appear to be outside of core space."
                );
                text.addPara(
                        "\"Among the many benefits of Commomwealth membership is access to the locations " +
                                "of these sites, which we guarantee hold at something of interest to you. Of particular " +
                                "note is the average number of Universal Access Chips that these caches tend to " +
                                "contain. Some of these you will absolutely not find anywhere else in the whole sector. " +
                                "Well, at least outside the autofactories of the major powers,\" " + getHeOrShe() + " " +
                                "adds with a grin."
                );
                text.addPara(
                        "\"Of course,\" " + getHeOrShe() + " points out awkwardly, \"there is the matter of the matter of the " +
                                "nominal membership fee."
                );

                options.addOption("\"Membership fee?\"", OptionId.INTRO_3);
                break;
            case INTRO_3:
                text.addPara(
                        "\"Unfortunately so,\" " + getHeOrShe() + " explains. \"We need to keep this material out of the hands of " +
                                "those who would use it to increase their stranglehold on the sector, or criminality " +
                                "of various degrees.\""
                );
                text.addPara(
                        "\"Our fee isn't the perfect filter, but it's well beyond the means of pirates, terrorists and the sorts " +
                                "of minor faction functionaries who'd frequent a place like this. And of course, it helps " +
                                "fund our continued operations to uncover more of these lost riches."
                );
                text.addPara(
                        Misc.ucFirst(getHeOrShe()) + " draws in a breath, conscious of the cost of what " + getHeOrShe() + "'s " +
                                "trying to sell. \"Our membership fee is one million credits. If you're comfortable " +
                                "with that, the Vesperon Combine will be happy to count you amongst its members."
                );
                text.addPara("This also gets you the location of one of our current mid-value finds.");


                options.addOption("\"Ouch... and that covers just one site's location?\"", OptionId.INTRO_4);
                break;
            case INTRO_4:
                text.addPara(
                        "\"That's right. But membership gets you access to our agents across the sector, and " +
                                "intel on future site locations will be somewhat less expensive."
                );
                text.addPara(
                        "\"Well, that's the end of my spiel,\" " + getHeOrShe() + " says, not quite sure how to read your expression." +
                                "\"Would you like to become a member of the Combine?\""
                );
                options.addOption("Agree and pay the one million credit membership fee", OptionId.PAY_FEE);
                options.addOption("Explain you can't afford it at the moment", OptionId.PAY_LATER);
                options.addOption(
                        "Tell " + getHimOrHer() + " to go and look for some other sap to fleece and leave",
                        OptionId.LEAVE
                );
                int credits = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
                if (credits < MEMBERSHIP_FEE) {
                    options.setEnabled(OptionId.PAY_FEE, false);
                    options.setTooltip(OptionId.PAY_FEE, "You cannot afford this at the moment.");
                }
                break;
            case PAY_FEE:

                text.addPara(
                        "\"Excellent!\" " + getHeOrShe() + " exclaims, barely able to conceal " + getHisOrHer() +
                                "excitement at what you assume must be a sizable commission. After an exchange of " +
                                "details, you deposit the credits into an account and receive your membership chip."
                );

                Global.getSector().getPlayerFleet().getCargo().getCredits().subtract((float)MEMBERSHIP_FEE);
                text.setFontSmallInsignia();
                text.addParagraph("Lost " + (int) MEMBERSHIP_FEE + " credits", Color.RED);
                text.highlightLastInLastPara("" + (int) MEMBERSHIP_FEE, Misc.getHighlightColor());
                text.setFontInsignia();

                text.addPara(
                        "\"Take good care of that,\" the representative says carefully. \"You'll need it to authorise " +
                                "our agents to speak to you.\"" +
                                Misc.ucFirst(getHeOrShe()) + " taps a few buttons on " + getHisOrHer() + " terminal, proceeds with" +
                                "more biometric scans, and then closes it with a smile. \"As promised, the location of " +
                                "the first site.\" Your TriPad pings to illustrate " + getHisOrHer() + " point. "
                );
                options.addOption("Inquire where one can find Vesperon's agents", OptionId.FIND_AGENTS);
                break;
            case FIND_AGENTS:

                text.addPara(
                    "\"You can find our people listed in the comm directories of most independent stations and colonies,\" " +
                        getHeOrShe() + " explains. \"We tend to steer clear of places run by the major powers.\""
                );
                options.addOption("Ask where Vesperon operates from", OptionId.FIND_BASE);
                break;
            case FIND_BASE:

                text.addPara(
                    getPerson().getName().getFirst() + " smiles. \"We have a presence beyond what you'd " +
                    "consider core space, but we're not quite ready to reveal ourselves just yet. That might change in " +
                    "future."
                );
                text.addPara(
                    "\"The value of our intelligence network demands a certain level of secrecy, and suffice " +
                        "to say we wouldn't survive an incursion by the Hegemony, the League, or any other major power if " +
                        "they knew the location of our tech mining operation and decided to 'liberate' it for themselves.\""
                );
                options.addOption("Ask for any other advice the rep can give", OptionId.ANY_ADVICE);
                break;
            case ANY_ADVICE:
                text.addPara(
                    "\"These facilities tend to be hidden around asteroid belts, so it's a good idea to start " +
                        "combing them. They were built specifically to avoid detection, so you'll need a specific " +
                        "scanning pulse frequency to find them among the rocks. Our dossiers contain that information."
                );
                text.addPara(
                    "\"We don't know who built them, but they must have had deep pockets. We analysed a few " +
                        "of these sites and determined they were built shortly after the Collapse, which infers that " +
                        "they were built to hoard technology from the peak of the Domain."
                );
                text.addPara(
                    "\"Just remember, some of them may have other dangers to contend with. One of the first facilities " +
                        "we found was abandoned but had active security measures.\""
                );
                options.addOption("Begin the hunt", OptionId.LEAVE_DONE);
                break;
            case LEAVE_DONE:
                text.addPara("Stay safe, my friend, and good hunting! The Combine wishes you luck.");

                vesperonIntelManager.setPlayerVesperonMember(true);
                vesperonIntelManager.distributeMembershipAgents();
                vesperonIntelManager.createVesperonMission(VesperonTags.VesperonCache.MID_LEVEL_CACHE);

                BarEventManager.getInstance().notifyWasInteractedWith(this);
                this.shouldRemoveEvent();

                options.addOption("Leave", OptionId.LEAVE);
                break;
            case PAY_LATER:
                text.addPara(
                        Misc.ucFirst(getHeOrShe()) + " smiles apologetically. \"I know it's a steep fee. " +
                            "But trust me, it's worth every credit. When you decide membership is right for you, " +
                            "look me up - I'm sure we'll meet again.\""
                );
                options.addOption("Shake hands and leave", OptionId.LEAVE);
                break;
            case LEAVE:
                done = true;
                break;
        }
    }

    @Override
    protected String getPersonFaction() {
        return VesperonTags.VESPERON_FACTION;
    }

    @Override
    protected String getPersonRank() {
        return Ranks.SPACE_SAILOR;
    }

    @Override
    protected String getPersonPost() {
        return Ranks.CITIZEN;
    }

    @Override
    protected Gender getPersonGender() {
        return Gender.ANY;
    }

}


