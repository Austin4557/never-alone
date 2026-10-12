package com.neveralone.forge;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;

public final class NamedArmorItem extends ArmorItem {
    private final String armorSet;
    public NamedArmorItem(String armorSet, ArmorMaterial material, Type type, Item.Properties properties) {
        super(material, type, properties);
        this.armorSet = armorSet;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        String suffix = slot == EquipmentSlot.LEGS ? "_layer_2" : "_layer_1";
        return "never_alone:textures/models/armor/" + armorSet + suffix + ".png";
    }
}
