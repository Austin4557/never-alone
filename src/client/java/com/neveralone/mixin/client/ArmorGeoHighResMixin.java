package com.neveralone.mixin.client;

import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.rpg_foundation.armor_api.client.geo.GeoBaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Armor Model API bakes box UV spans at 1 texel per model unit even when the
 * declared atlas is 256x256. Never Alone armor atlases are authored at 4x the
 * conventional 64px density, so scale the UV span without changing geometry.
 */
@Mixin(value = GeoBaker.class, remap = false)
public abstract class ArmorGeoHighResMixin {
    @ModifyArgs(
        method = "addCuboid",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/model/geom/builders/CubeListBuilder;addBox(FFFFFFLnet/minecraft/client/model/geom/builders/CubeDeformation;FF)Lnet/minecraft/client/model/geom/builders/CubeListBuilder;"
        )
    )
    private static void neverAlone$fourXArmorTexelDensity(Args args) {
        args.set(7, 4.0F);
        args.set(8, 4.0F);
    }
}
