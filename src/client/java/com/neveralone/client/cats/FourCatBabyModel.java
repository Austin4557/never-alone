package com.neveralone.client.cats;

import net.minecraft.client.model.animal.feline.BabyCatModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.util.Mth;

/** Custom kitten model preserving vanilla baby movement plus relaxed personality. */
public final class FourCatBabyModel extends BabyCatModel {
    private final ModelPart head;
    private final ModelPart tail1;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;

    public FourCatBabyModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.tail1 = root.getChild("tail1");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
    }

    @Override
    public void setupAnim(CatRenderState baseState) {
        super.setupAnim(baseState);
        if (!(baseState instanceof FourCatRenderState state) || state.identity == null || !state.isSettled()) return;

        float time = state.ageInTicks;
        float phase = (state.animationSeed & 31) * 0.37F;
        head.yRot += Mth.sin(time * 0.065F + phase) * 0.13F;
        tail1.yRot += Mth.sin(time * 0.14F + phase) * 0.24F;

        if (state.shouldMakeBiscuits()) {
            float knead = Mth.sin(time * 0.36F + phase);
            float left = Math.max(0.0F, knead);
            float right = Math.max(0.0F, -knead);
            leftFrontLeg.xRot -= left * 0.24F;
            rightFrontLeg.xRot -= right * 0.24F;
            leftFrontLeg.y += left * 0.32F * state.ageScale;
            rightFrontLeg.y += right * 0.32F * state.ageScale;
        }
    }
}
