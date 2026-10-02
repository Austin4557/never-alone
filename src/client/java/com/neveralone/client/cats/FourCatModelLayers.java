package com.neveralone.client.cats;

import com.neveralone.NeverAlone;
import net.minecraft.client.model.animal.feline.AdultCatModel;
import net.minecraft.client.model.animal.feline.AdultFelineModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class FourCatModelLayers {
    public static final ModelLayerLocation OSCAR = layer("oscar");
    public static final ModelLayerLocation DRAKO = layer("drako");
    public static final ModelLayerLocation KLOUSE = layer("klouse");
    public static final ModelLayerLocation LUCY = layer("lucy");

    private FourCatModelLayers() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(NeverAlone.id("cat/" + name), "main");
    }

    private static LayerDefinition base(float deformation) {
        return LayerDefinition.create(
            AdultFelineModel.createBodyMesh(new CubeDeformation(deformation)), 64, 32
        ).apply(AdultCatModel.CAT_TRANSFORMER);
    }

    public static LayerDefinition createOscar() {
        MeshDefinition mesh = AdultFelineModel.createBodyMesh(new CubeDeformation(0.42F));
        PartDefinition root = mesh.getRoot();
        // Extra ruff and cheek fluff ride with the animated body/head.
        root.getChild("head").addOrReplaceChild("oscar_cheeks",
            CubeListBuilder.create().texOffs(0, 26).addBox(-3.0F, -1.2F, -2.3F, 6.0F, 3.6F, 4.4F, new CubeDeformation(0.12F)),
            PartPose.ZERO);
        root.getChild("body").addOrReplaceChild("oscar_ruff",
            CubeListBuilder.create().texOffs(24, 24).addBox(-2.8F, 2.0F, -8.7F, 5.6F, 5.0F, 3.2F, new CubeDeformation(0.18F)),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64).apply(AdultCatModel.CAT_TRANSFORMER);
    }

    public static LayerDefinition createDrako() {
        // Wider/rounder silhouette, but intentionally no long-fur add-ons.
        return base(0.28F);
    }

    public static LayerDefinition createKlouse() {
        MeshDefinition mesh = AdultFelineModel.createBodyMesh(new CubeDeformation(0.34F));
        PartDefinition root = mesh.getRoot();
        root.getChild("body").addOrReplaceChild("klouse_chest",
            CubeListBuilder.create().texOffs(24, 24).addBox(-2.7F, 1.5F, -9.0F, 5.4F, 6.2F, 3.4F, new CubeDeformation(0.20F)),
            PartPose.ZERO);
        root.getChild("tail1").addOrReplaceChild("klouse_tail_fluff",
            CubeListBuilder.create().texOffs(0, 34).addBox(-1.15F, -0.2F, -0.6F, 2.3F, 8.4F, 2.3F, new CubeDeformation(0.08F)),
            PartPose.ZERO);
        root.getChild("tail2").addOrReplaceChild("klouse_tail_tip_fluff",
            CubeListBuilder.create().texOffs(10, 34).addBox(-1.1F, -0.2F, -0.6F, 2.2F, 8.4F, 2.2F, new CubeDeformation(0.08F)),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64).apply(AdultCatModel.CAT_TRANSFORMER);
    }

    public static LayerDefinition createLucy() {
        // Slight inward deformation keeps her around vanilla scale but visibly leaner.
        return base(-0.12F);
    }
}
