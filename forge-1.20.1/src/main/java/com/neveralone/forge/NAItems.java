package com.neveralone.forge;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class NAItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NeverAloneForge.ID);
    public static final Map<String, Map<String, RegistryObject<Item>>> ARMOR = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> MATERIALS = new LinkedHashMap<>();
    static {
        registerSet("rhinestone", "tiara");
        registerSet("emerald_warden", "helmet");
        registerSet("dark_amethyst", "helmet");
        registerIngredient("radiant_rhinestone");
        registerIngredient("resonant_emerald");
        registerIngredient("dark_amethyst_crystal");
    }
    private static void registerIngredient(String key) {
        MATERIALS.put(key, ITEMS.register(key, () -> new Item(new Item.Properties())));
    }
    private static void registerSet(String set, String headName) {
        Map<String, RegistryObject<Item>> parts = new LinkedHashMap<>();
        NAArmorMaterials armorMaterial = set.equals("rhinestone") ? NAArmorMaterials.RHINESTONE
            : set.equals("emerald_warden") ? NAArmorMaterials.EMERALD_WARDEN
            : NAArmorMaterials.DARK_AMETHYST;
        parts.put("head", ITEMS.register(set + "_" + headName,
            () -> new NamedArmorItem(set, armorMaterial, ArmorItem.Type.HELMET, new Item.Properties())));
        parts.put("chest", ITEMS.register(set + "_chestplate",
            () -> new NamedArmorItem(set, armorMaterial, ArmorItem.Type.CHESTPLATE, new Item.Properties())));
        parts.put("legs", ITEMS.register(set + "_leggings",
            () -> new NamedArmorItem(set, armorMaterial, ArmorItem.Type.LEGGINGS, new Item.Properties())));
        parts.put("feet", ITEMS.register(set + "_boots",
            () -> new NamedArmorItem(set, armorMaterial, ArmorItem.Type.BOOTS, new Item.Properties())));
        ARMOR.put(set, parts);
    }
    private NAItems() { }
}
