package com.neveralone.client.cats;

import net.minecraft.client.renderer.entity.state.CatRenderState;

/**
 * Per-entity render data for the four custom cats.
 * Keeping this on the render state prevents identity/animation data leaking
 * between cats when several are rendered in the same frame.
 */
public final class FourCatRenderState extends CatRenderState {
    public FourCatIdentity identity;
    public int animationSeed;

    public boolean isCustomCat() {
        return identity != null;
    }

    public boolean isSettled() {
        return (isSitting || lieDownAmount > 0.05F || relaxStateOneAmount > 0.05F)
            && walkAnimationSpeed < 0.05F;
    }

    /**
     * Kneading comes in relaxed bursts instead of looping constantly.
     * Entity id offsets the cycle so several cats do not synchronize.
     */
    public float biscuitWeight() {
        if (identity == null || !identity.makesBiscuits() || !isSettled()) return 0.0F;

        float cycle = (ageInTicks + Math.floorMod(animationSeed * 37, 240)) % 240.0F;
        if (cycle >= 92.0F) return 0.0F;

        // Ease in/out over ten ticks, then spend the middle of the window at full weight.
        float fadeIn = Math.min(1.0F, cycle / 10.0F);
        float fadeOut = Math.min(1.0F, (92.0F - cycle) / 10.0F);
        return Math.max(0.0F, Math.min(fadeIn, fadeOut));
    }

    public boolean shouldMakeBiscuits() {
        return biscuitWeight() > 0.0F;
    }
}
