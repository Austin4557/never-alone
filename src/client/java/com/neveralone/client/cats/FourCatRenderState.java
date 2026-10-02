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
        return isSitting || lieDownAmount > 0.05F || relaxStateOneAmount > 0.05F;
    }

    public boolean shouldMakeBiscuits() {
        return identity != null && identity.makesBiscuits() && isSettled();
    }
}
