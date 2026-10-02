package com.neveralone.armor;

import com.neveralone.NeverAlone;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class DarkAmethystArmorMaterial {
    private DarkAmethystArmorMaterial() {}

    public static final int BASE_DURABILITY = 40;

    public static final TagKey<Item> REPAIRS_DARK_AMETHYST =
            TagKey.create(BuiltInRegistries.ITEM.key(), NeverAlone.id("repairs_dark_amethyst"));

    public static final ResourceKey<EquipmentAsset> ASSET_KEY =
            ResourceKey.create(EquipmentAssets.ROOT_ID, NeverAlone.id("dark_amethyst"));

    public static final ArmorMaterial INSTANCE = new ArmorMaterial(
            BASE_DURABILITY,
            Map.of(
                    ArmorType.HELMET, 3,
                    ArmorType.CHESTPLATE, 8,
                    ArmorType.LEGGINGS, 6,
                    ArmorType.BOOTS, 3
            ),
            18,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.0F,
            0.10F,
            REPAIRS_DARK_AMETHYST,
            ASSET_KEY
    );
}
