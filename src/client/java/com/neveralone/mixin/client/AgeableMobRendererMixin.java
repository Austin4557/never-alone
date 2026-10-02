package com.neveralone.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.neveralone.client.cats.FourCatModelProvider;
import com.neveralone.client.cats.FourCatRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class AgeableMobRendererMixin {
    @Shadow
    protected EntityModel<?> model;

    @Inject(method = "submit", at = @At("HEAD"))
    private void neverAlone$selectCustomCatModel(
        LivingEntityRenderState baseState,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraRenderState camera,
        CallbackInfo ci
    ) {
        if ((Object) this instanceof FourCatModelProvider provider
            && baseState instanceof FourCatRenderState state
            && state.identity != null) {
            this.model = provider.neverAlone$modelFor(state.identity, state.isBaby);
        }
    }
}
