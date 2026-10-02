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
    public static final ModelLayerLocation OSCAR_BABY = layer("oscar_baby");
    public static final ModelLayerLocation DRAKO_BABY = layer("drako_baby");
    public static final ModelLayerLocation KLOUSE_BABY = layer("klouse_baby");
    public static final ModelLayerLocation LUCY_BABY = layer("lucy_baby");

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
        root.getChild("tail1").addOrReplaceChild("oscar_tail_fluff",
            CubeListBuilder.create().texOffs(0, 34).addBox(-1.25F, -0.25F, -0.7F, 2.5F, 8.5F, 2.5F, new CubeDeformation(0.10F)),
            PartPose.ZERO);
        root.getChild("tail2").addOrReplaceChild("oscar_tail_tip_fluff",
            CubeListBuilder.create().texOffs(12, 34).addBox(-1.2F, -0.25F, -0.7F, 2.4F, 8.5F, 2.4F, new CubeDeformation(0.10F)),
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
        // Klouse is not only wider: a body-length overlay extends his silhouette
        // along the body's local Y axis (the adult body is rotated 90 degrees).
        root.getChild("body").addOrReplaceChild("klouse_long_body",
            CubeListBuilder.create().texOffs(32, 34).addBox(-2.35F, -0.7F, -2.9F, 4.7F, 17.4F, 6.2F, new CubeDeformation(0.10F)),
            PartPose.ZERO);
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

    private static MeshDefinition babyMesh() {
        // BabyFelineModel exposes its finished LayerDefinition rather than the
        // mesh directly, so custom kitten meshes mirror the vanilla 26.2 layout.
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F,-3.0F,-2.875F,5.0F,4.0F,4.0F).texOffs(18,0).addBox(-2.0F,-4.0F,-0.875F,1.0F,1.0F,2.0F).texOffs(24,0).addBox(1.0F,-4.0F,-0.875F,1.0F,1.0F,2.0F).texOffs(18,3).addBox(-1.5F,-1.0F,-3.875F,3.0F,2.0F,1.0F), PartPose.offset(0.0F,20.0F,-3.125F));
        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(18,18).addBox(-0.5F,0.0F,-1.0F,1.0F,2.0F,2.0F), PartPose.offset(1.0F,22.0F,-1.5F));
        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(12,18).addBox(-0.5F,0.0F,-1.0F,1.0F,2.0F,2.0F), PartPose.offset(-1.0F,22.0F,-1.5F));
        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(18,22).addBox(-0.5F,0.0F,-1.0F,1.0F,2.0F,2.0F), PartPose.offset(1.0F,22.0F,2.5F));
        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(12,22).addBox(-0.5F,0.0F,-1.0F,1.0F,2.0F,2.0F), PartPose.offset(-1.0F,22.0F,2.5F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0,8).addBox(-2.0F,-1.5F,-3.5F,4.0F,3.0F,7.0F), PartPose.offset(0.0F,20.5F,0.5F));
        root.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(0,18).addBox(-0.5F,-0.107F,0.0849F,1.0F,1.0F,5.0F), PartPose.offsetAndRotation(0.0F,19.107F,3.9151F,-0.567232F,0.0F,0.0F));
        root.addOrReplaceChild("tail2", CubeListBuilder.create(), PartPose.ZERO);
        return mesh;
    }

    private static LayerDefinition baby(float puff, boolean chestFluff, boolean tailFluff) {
        MeshDefinition mesh = babyMesh();
        PartDefinition root = mesh.getRoot();
        if (puff != 0.0F) {
            root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0,8).addBox(-2.0F,-1.5F,-3.5F,4.0F,3.0F,7.0F,new CubeDeformation(puff)), PartPose.offset(0.0F,20.5F,0.5F));
        }
        if (chestFluff) {
            root.getChild("body").addOrReplaceChild("baby_chest_fluff", CubeListBuilder.create().texOffs(0,27).addBox(-2.15F,-1.7F,-3.8F,4.3F,3.3F,2.2F,new CubeDeformation(0.08F)), PartPose.ZERO);
        }
        if (tailFluff) {
            root.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(0,18).addBox(-0.9F,-0.107F,-0.25F,1.8F,1.8F,5.4F,new CubeDeformation(0.05F)), PartPose.offsetAndRotation(0.0F,19.107F,3.9151F,-0.567232F,0.0F,0.0F));
        }
        return LayerDefinition.create(mesh,64,64);
    }

    public static LayerDefinition createOscarBaby() { return baby(0.28F, true, true); }
    public static LayerDefinition createDrakoBaby() { return baby(0.18F, false, false); }
    public static LayerDefinition createKlouseBaby() { return baby(0.22F, true, true); }
    public static LayerDefinition createLucyBaby() { return baby(-0.08F, false, false); }

    public static LayerDefinition createLucy() {
        // Slight inward deformation keeps her around vanilla scale but visibly leaner.
        return base(-0.12F);
    }
}
