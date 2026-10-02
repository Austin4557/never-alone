package com.neveralone.client.cats;

import java.util.Locale;
import net.minecraft.resources.Identifier;

import com.neveralone.NeverAlone;

/**
 * Stable identity profiles for Austin's four cats.
 *
 * Identity is deliberately name-based so these appearances can coexist with
 * every vanilla cat variant without replacing vanilla cat data.
 */
public enum FourCatIdentity {
    OSCAR("Oscar", "oscar", BodyType.FLUFFY_CHONK, false),
    DRAKO("Drako", "drako", BodyType.SLEEK_CHONK, true),
    KLOUSE("Klouse", "klouse", BodyType.LONG_FLUFFY_CHONK, true),
    LUCY("Lucy", "lucy", BodyType.SLIM, true);

    public enum BodyType {
        FLUFFY_CHONK,
        SLEEK_CHONK,
        LONG_FLUFFY_CHONK,
        SLIM
    }

    private final String displayName;
    private final String assetName;
    private final BodyType bodyType;
    private final boolean makesBiscuits;

    FourCatIdentity(String displayName, String assetName, BodyType bodyType, boolean makesBiscuits) {
        this.displayName = displayName;
        this.assetName = assetName;
        this.bodyType = bodyType;
        this.makesBiscuits = makesBiscuits;
    }

    public String displayName() {
        return displayName;
    }

    public BodyType bodyType() {
        return bodyType;
    }

    public boolean makesBiscuits() {
        return makesBiscuits;
    }

    public Identifier texture(boolean baby) {
        return NeverAlone.id("textures/entity/cat/" + assetName + (baby ? "_baby" : "") + ".png");
    }

    public static FourCatIdentity fromName(String rawName) {
        if (rawName == null) return null;
        String normalized = rawName.strip().toLowerCase(Locale.ROOT);
        for (FourCatIdentity identity : values()) {
            if (identity.displayName.toLowerCase(Locale.ROOT).equals(normalized)) {
                return identity;
            }
        }
        return null;
    }
}
