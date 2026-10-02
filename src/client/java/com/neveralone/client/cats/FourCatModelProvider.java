package com.neveralone.client.cats;

import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.renderer.entity.state.CatRenderState;

public interface FourCatModelProvider {
    AbstractFelineModel<CatRenderState> neverAlone$modelFor(FourCatIdentity identity);
}
