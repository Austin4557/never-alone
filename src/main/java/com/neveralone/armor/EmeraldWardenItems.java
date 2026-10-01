package com.neveralone.armor;

import com.neveralone.NeverAlone;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

public final class EmeraldWardenItems {
    private EmeraldWardenItems() {}
    public static final Item RESONANT_EMERALD = register("resonant_emerald", Item::new, new Item.Properties());
    public static final Item EMERALD_WARDEN_HELMET = armor("emerald_warden_helmet", ArmorType.HELMET);
    public static final Item EMERALD_WARDEN_CHESTPLATE = armor("emerald_warden_chestplate", ArmorType.CHESTPLATE);
    public static final Item EMERALD_WARDEN_LEGGINGS = armor("emerald_warden_leggings", ArmorType.LEGGINGS);
    public static final Item EMERALD_WARDEN_BOOTS = armor("emerald_warden_boots", ArmorType.BOOTS);
    private static Item armor(String name, ArmorType type) {
        return register(name, Item::new, new Item.Properties().humanoidArmor(EmeraldWardenArmorMaterial.INSTANCE,type).durability(type.getDurability(EmeraldWardenArmorMaterial.BASE_DURABILITY)));
    }
    private static Item register(String name, Function<Item.Properties,Item> factory, Item.Properties properties) {
        ResourceKey<Item> key=ResourceKey.create(BuiltInRegistries.ITEM.key(),NeverAlone.id(name));
        return Registry.register(BuiltInRegistries.ITEM,key,factory.apply(properties.setId(key)));
    }
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(e->e.accept(RESONANT_EMERALD));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(e->{e.accept(EMERALD_WARDEN_HELMET);e.accept(EMERALD_WARDEN_CHESTPLATE);e.accept(EMERALD_WARDEN_LEGGINGS);e.accept(EMERALD_WARDEN_BOOTS);});
    }
}