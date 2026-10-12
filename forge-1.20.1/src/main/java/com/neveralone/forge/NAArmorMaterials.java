package com.neveralone.forge;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 1.20.1 Forge implementation of the approved Fabric 26.2 armor stats.
 * The material interface differs between Minecraft 1.20.1 and 26.2.
 */
public enum NAArmorMaterials implements ArmorMaterial {
    RHINESTONE("rhinestone", 37, 22, 2.5F, 0.06F, SoundEvents.ARMOR_EQUIP_DIAMOND),
    EMERALD_WARDEN("emerald_warden", 41, 17, 3.0F, 0.09F, SoundEvents.ARMOR_EQUIP_NETHERITE),
    DARK_AMETHYST("dark_amethyst", 40, 18, 3.0F, 0.10F, SoundEvents.ARMOR_EQUIP_NETHERITE);

    private final String name;
    private final int durabilityMultiplier;
    private final int enchantability;
    private final float toughness;
    private final float knockbackResistance;
    private final SoundEvent equipSound;

    NAArmorMaterials(String name, int durabilityMultiplier, int enchantability,
                     float toughness, float knockbackResistance, SoundEvent equipSound) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.enchantability = enchantability;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.equipSound = equipSound;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        // These multipliers match 1.20.1's canonical armor piece multipliers.
        switch (type) {
            case HELMET: return 11 * durabilityMultiplier;
            case CHESTPLATE: return 16 * durabilityMultiplier;
            case LEGGINGS: return 15 * durabilityMultiplier;
            case BOOTS: return 13 * durabilityMultiplier;
            default: return 11 * durabilityMultiplier;
        }
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        switch (type) {
            case HELMET: return 3;
            case CHESTPLATE: return 8;
            case LEGGINGS: return 6;
            case BOOTS: return 3;
            default: return 0;
        }
    }

    @Override public int getEnchantmentValue() { return enchantability; }
    @Override public SoundEvent getEquipSound() { return equipSound; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackResistance; }
    @Override public String getName() { return NeverAloneForge.ID + ":" + name; }

    @Override
    public Ingredient getRepairIngredient() {
        switch (this) {
            case RHINESTONE: return Ingredient.of(NAItems.MATERIALS.get("radiant_rhinestone").get());
            case EMERALD_WARDEN: return Ingredient.of(NAItems.MATERIALS.get("resonant_emerald").get());
            case DARK_AMETHYST: return Ingredient.of(NAItems.MATERIALS.get("dark_amethyst_crystal").get());
            default: return Ingredient.EMPTY;
        }
    }
}
