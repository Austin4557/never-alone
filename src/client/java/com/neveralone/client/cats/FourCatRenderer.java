package com.neveralone.client.cats;

import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.feline.Cat;

/**
 * First-stage renderer hook. Keeps vanilla cat models/animations/collars while
 * swapping the texture for one of the four named cats. Custom geometry and
 * relaxed animation layers build on this once the hook is compile-verified.
 */
public final class FourCatRenderer extends CatRenderer {
    private FourCatIdentity currentIdentity;

    public FourCatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(Cat entity, CatRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        currentIdentity = entity.hasCustomName()
            ? FourCatIdentity.fromName(entity.getCustomName().getString())
            : null;
        if (currentIdentity != null) {
            state.texture = currentIdentity.texture();
        }
    }

    @Override
    public Identifier getTextureLocation(CatRenderState state) {
        return state.texture;
    }

    public FourCatIdentity currentIdentity() {
        return currentIdentity;
    }
}
