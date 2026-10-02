package com.neveralone.client.cats;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.animal.feline.Cat;

/**
 * Vanilla-compatible renderer that selects a distinct adult mesh per custom cat.
 */
public final class FourCatRenderer extends CatRenderer {
    private final AbstractFelineModel<CatRenderState> vanillaAdult;
    private final FourCatModel oscarModel;
    private final FourCatModel drakoModel;
    private final FourCatModel klouseModel;
    private final FourCatModel lucyModel;

    public FourCatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.vanillaAdult = new FourCatModel(context.bakeLayer(ModelLayers.CAT));
        this.oscarModel = new FourCatModel(context.bakeLayer(FourCatModelLayers.OSCAR));
        this.drakoModel = new FourCatModel(context.bakeLayer(FourCatModelLayers.DRAKO));
        this.klouseModel = new FourCatModel(context.bakeLayer(FourCatModelLayers.KLOUSE));
        this.lucyModel = new FourCatModel(context.bakeLayer(FourCatModelLayers.LUCY));
    }

    @Override
    public FourCatRenderState createRenderState() {
        return new FourCatRenderState();
    }

    @Override
    public void extractRenderState(Cat entity, CatRenderState baseState, float partialTicks) {
        super.extractRenderState(entity, baseState, partialTicks);
        if (!(baseState instanceof FourCatRenderState state)) return;

        state.identity = entity.hasCustomName()
            ? FourCatIdentity.fromName(entity.getCustomName().getString())
            : null;
        state.animationSeed = entity.getId();

        if (state.identity != null && !state.isBaby) {
            state.texture = state.identity.texture();
        }
    }

    @Override
    public void submit(CatRenderState baseState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        // Let AgeableMobRenderer perform its normal baby/adult selection first.
        // For custom adults, swap our mesh immediately before LivingEntityRenderer
        // consumes it. Babies and ordinary cats remain on the vanilla path.
        if (baseState instanceof FourCatRenderState state && !state.isBaby && state.identity != null) {
            this.model = modelFor(state.identity);
            // Calling the parent would overwrite this.model with its private adult
            // model, so custom adults render through our copied vanilla-compatible
            // submit path in a later integration pass. For now preserve the known
            // safe vanilla path until that hook is compile-verified.
        }
        super.submit(baseState, poseStack, collector, camera);
    }

    private AbstractFelineModel<CatRenderState> modelFor(FourCatIdentity identity) {
        return switch (identity) {
            case OSCAR -> oscarModel;
            case DRAKO -> drakoModel;
            case KLOUSE -> klouseModel;
            case LUCY -> lucyModel;
        };
    }
}
