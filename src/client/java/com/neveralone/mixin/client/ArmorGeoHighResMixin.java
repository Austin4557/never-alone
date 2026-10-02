package com.neveralone.mixin.client;

import net.rpg_foundation.armor_api.client.geo.GeoBaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.rpg_foundation.armor_api.client.geo.GeoModel;

/**
 * Gives only Never Alone's 256px armor atlases 4x box-UV density.
 * Other Armor Model API consumers remain at the API's normal 1x density.
 */
@Mixin(value = GeoBaker.class, remap = false)
public abstract class ArmorGeoHighResMixin {
    @Unique private static final ThreadLocal<Boolean> neverAlone$highRes = ThreadLocal.withInitial(() -> false);

    @Inject(method = "bake", at = @At("HEAD"))
    private static void neverAlone$beginHighRes(GeoModel model, String source, CallbackInfoReturnable<LayerDefinition> cir) {
        neverAlone$highRes.set(source != null
            && source.startsWith("never_alone:")
            && model.textureWidth() == 256
            && model.textureHeight() == 256);
    }

    @ModifyArgs(
        method = "addCuboid",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/builders/CubeListBuilder;addBox(FFFFFFLnet/minecraft/client/model/geom/builders/CubeDeformation;FF)Lnet/minecraft/client/model/geom/builders/CubeListBuilder;"
        )
    )
    private static void neverAlone$fourXArmorTexelDensity(Args args) {
        if (neverAlone$highRes.get()) {
            args.set(7, 4.0F);
            args.set(8, 4.0F);
        }
    }

    @Inject(method = "bake", at = @At("RETURN"))
    private static void neverAlone$endHighRes(GeoModel model, String source, CallbackInfoReturnable<LayerDefinition> cir) {
        neverAlone$highRes.remove();
    }
}
