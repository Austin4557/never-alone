package com.neveralone.client.cats;

import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.world.entity.animal.feline.Cat;

/** Vanilla-compatible renderer providing eight custom cat meshes. */
public final class FourCatRenderer extends CatRenderer implements FourCatModelProvider {
    private final FourCatModel oscarModel;
    private final FourCatModel drakoModel;
    private final FourCatModel klouseModel;
    private final FourCatModel lucyModel;
    private final FourCatBabyModel oscarBabyModel;
    private final FourCatBabyModel drakoBabyModel;
    private final FourCatBabyModel klouseBabyModel;
    private final FourCatBabyModel lucyBabyModel;

    public FourCatRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.oscarModel = new FourCatModel(context.bakeLayer(FourCatModelLayers.OSCAR));
        this.drakoModel = new FourCatModel(context.bakeLayer(FourCatModelLayers.DRAKO));
        this.klouseModel = new FourCatModel(context.bakeLayer(FourCatModelLayers.KLOUSE));
        this.lucyModel = new FourCatModel(context.bakeLayer(FourCatModelLayers.LUCY));
        this.oscarBabyModel = new FourCatBabyModel(context.bakeLayer(FourCatModelLayers.OSCAR_BABY));
        this.drakoBabyModel = new FourCatBabyModel(context.bakeLayer(FourCatModelLayers.DRAKO_BABY));
        this.klouseBabyModel = new FourCatBabyModel(context.bakeLayer(FourCatModelLayers.KLOUSE_BABY));
        this.lucyBabyModel = new FourCatBabyModel(context.bakeLayer(FourCatModelLayers.LUCY_BABY));
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

        if (state.identity != null) {
            state.texture = state.identity.texture();
        }
    }

    @Override
    public AbstractFelineModel<CatRenderState> neverAlone$modelFor(FourCatIdentity identity, boolean baby) {
        if (baby) {
            return switch (identity) {
                case OSCAR -> oscarBabyModel;
                case DRAKO -> drakoBabyModel;
                case KLOUSE -> klouseBabyModel;
                case LUCY -> lucyBabyModel;
            };
        }
        return switch (identity) {
            case OSCAR -> oscarModel;
            case DRAKO -> drakoModel;
            case KLOUSE -> klouseModel;
            case LUCY -> lucyModel;
        };
    }
}
