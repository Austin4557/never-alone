package com.neveralone.client.cats;

import net.minecraft.client.model.animal.feline.AdultCatModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Adult model animation pass for the four custom cats.
 *
 * Vanilla animation runs first. We only add personality motion while the cat
 * is settled, so walking/sprinting/combat movement remains untouched.
 */
public final class FourCatModel extends AdultCatModel {
    private final ModelPart head;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;

    public FourCatModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.tail1 = root.getChild("tail1");
        this.tail2 = root.getChild("tail2");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
    }

    @Override
    public void setupAnim(net.minecraft.client.renderer.entity.state.CatRenderState baseState) {
        super.setupAnim(baseState);
        if (!(baseState instanceof FourCatRenderState state) || !state.isCustomCat() || state.isBaby || !state.isSettled()) {
            return;
        }

        float time = state.ageInTicks;
        float phase = (state.animationSeed & 31) * 0.37F;

        // Slow, subtle shared relaxed idle.
        float headTurn = Mth.sin(time * 0.055F + phase) * 0.16F;
        float tailSwish = Mth.sin(time * 0.11F + phase) * 0.22F;
        head.yRot += headTurn;
        tail1.yRot += tailSwish * 0.35F;
        tail2.yRot += tailSwish;

        // Drako, Klouse and Lucy knead while settled. Oscar's identity profile
        // has makesBiscuits=false, so he can never enter this animation.
        float biscuitWeight = state.biscuitWeight();
        if (biscuitWeight > 0.0F) {
            float knead = Mth.sin(time * 0.32F + phase);
            float left = Math.max(0.0F, knead);
            float right = Math.max(0.0F, -knead);

            leftFrontLeg.xRot -= left * 0.30F * biscuitWeight;
            rightFrontLeg.xRot -= right * 0.30F * biscuitWeight;
            leftFrontLeg.y += left * 0.55F * state.ageScale * biscuitWeight;
            rightFrontLeg.y += right * 0.55F * state.ageScale * biscuitWeight;
        }
    }
}
