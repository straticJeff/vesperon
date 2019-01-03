package org.stratic.fs.starsector.util;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.stratic.fs.starsector.api.impl.campaign.ids.VesperonTags;

import java.util.Random;

public class SequenceGenerator extends Random {

    private String key;

    public static SequenceGenerator get(String key) {
        key = "$gen_" + key;
        SequenceGenerator generator;
        Logger l = Global.getLogger(SequenceGenerator.class);
        if (!Global.getSector().getMemoryWithoutUpdate().contains(key)) {
            l.info("Creating new generator with key: " + key);
            generator = new SequenceGenerator(key);
            Global.getSector().getMemoryWithoutUpdate().set(key, generator);
        } else {
            l.info("Reloading generator with key: " + key);
            generator = (SequenceGenerator)Global.getSector().getMemoryWithoutUpdate().get(key);
        }
        return generator;
    }

    private SequenceGenerator(String key) {
        this.key = key;
    }

    public boolean nextBoolean() {
        boolean b = super.nextBoolean();
        updateMemory();
        return b;
    }

    public int nextInt(int bound) {
        int b = super.nextInt(bound);
        updateMemory();
        return b;
    }

    public long nextLong() {
        long b = super.nextLong();
        updateMemory();
        return b;
    }

    public float nextFloat() {
        float b = super.nextFloat();
        updateMemory();
        return b;
    }

    private void updateMemory() {
        Global.getSector().getMemoryWithoutUpdate().set(key, this);
    }
}
