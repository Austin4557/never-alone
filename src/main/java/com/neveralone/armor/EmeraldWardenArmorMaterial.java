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

public final class EmeraldWardenArmorMaterial {
    private EmeraldWardenArmorMaterial() {}
    public static final int BASE_DURABILITY = 41;
    public static final TagKey<Item> REPAIRS_EMERALD_WARDEN = TagKey.create(BuiltInRegistries.ITEM.key(), NeverAlone.id("repairs_emerald_warden"));
    public static final ResourceKey<EquipmentAsset> ASSET_KEY = ResourceKey.create(EquipmentAssets.ROOT_ID, NeverAlone.id("emerald_warden"));
    public static final ArmorMaterial INSTANCE = new ArmorMaterial(
        BASE_DURABILITY, Map.of(ArmorType.HELMET,3, ArmorType.CHESTPLATE,8, ArmorType.LEGGINGS,6, ArmorType.BOOTS,3),
        17, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.09F, REPAIRS_EMERALD_WARDEN, ASSET_KEY);
}