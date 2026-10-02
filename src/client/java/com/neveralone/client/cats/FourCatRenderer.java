package com.neveralone.client.cats;

import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.world.entity.animal.feline.Cat;

/**
 * Vanilla-compatible cat renderer with per-cat identity state.
 */
public final class FourCatRenderer extends CatRenderer {
    public FourCatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public FourCatRenderState createRenderState() {
        return new FourCatRenderState();
    }

    @Override
    public void extractRenderState(Cat entity, CatRenderState baseState, float partialTicks) {
        super.extractRenderState(entity, baseState, partialTicks);
        if (!(baseState instanceof FourCatRenderState state)) {
            return;
        }

        state.identity = entity.hasCustomName()
            ? FourCatIdentity.fromName(entity.getCustomName().getString())
            : null;
        state.animationSeed = entity.getId();

        // Keep babies visually vanilla for the first implementation pass.
        // Adult custom geometry/animations are deliberately not forced onto kittens.
        if (state.identity != null && !state.isBaby) {
            state.texture = state.identity.texture();
        }
    }
}
