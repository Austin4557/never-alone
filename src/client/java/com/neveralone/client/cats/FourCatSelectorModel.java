package com.neveralone.client.cats;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

/**
 * Adult model facade used by vanilla AgeableMobRenderer.
 * It chooses the actual adult mesh from the current render state while leaving
 * the renderer's vanilla baby/adult switching untouched.
 */
public final class FourCatSelectorModel extends AbstractFelineModel<CatRenderState> {
    private final AbstractFelineModel<CatRenderState> vanilla;
    private final AbstractFelineModel<CatRenderState> oscar;
    private final AbstractFelineModel<CatRenderState> drako;
    private final AbstractFelineModel<CatRenderState> klouse;
    private final AbstractFelineModel<CatRenderState> lucy;
    private AbstractFelineModel<CatRenderState> selected;

    public FourCatSelectorModel(
        ModelPart facadeRoot,
        AbstractFelineModel<CatRenderState> vanilla,
        AbstractFelineModel<CatRenderState> oscar,
        AbstractFelineModel<CatRenderState> drako,
        AbstractFelineModel<CatRenderState> klouse,
        AbstractFelineModel<CatRenderState> lucy
    ) {
        super(facadeRoot);
        this.vanilla = vanilla;
        this.oscar = oscar;
        this.drako = drako;
        this.klouse = klouse;
        this.lucy = lucy;
        this.selected = vanilla;
    }

    @Override
    public void setupAnim(CatRenderState state) {
        selected = select(state);
        selected.setupAnim(state);
    }

    private AbstractFelineModel<CatRenderState> select(CatRenderState state) {
        if (!(state instanceof FourCatRenderState custom) || custom.identity == null) return vanilla;
        return switch (custom.identity) {
            case OSCAR -> oscar;
            case DRAKO -> drako;
            case KLOUSE -> klouse;
            case LUCY -> lucy;
        };
    }
}
