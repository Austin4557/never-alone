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

public final class RhinestoneItems {
    private RhinestoneItems() {}
    public static final Item RADIANT_RHINESTONE = register("radiant_rhinestone", Item::new, new Item.Properties());
    public static final Item RHINESTONE_TIARA = armor("rhinestone_tiara", ArmorType.HELMET);
    public static final Item RHINESTONE_CHESTPLATE = armor("rhinestone_chestplate", ArmorType.CHESTPLATE);
    public static final Item RHINESTONE_LEGGINGS = armor("rhinestone_leggings", ArmorType.LEGGINGS);
    public static final Item RHINESTONE_BOOTS = armor("rhinestone_boots", ArmorType.BOOTS);
    private static Item armor(String name, ArmorType type) {
        return register(name, Item::new, new Item.Properties().humanoidArmor(RhinestoneArmorMaterial.INSTANCE,type).durability(type.getDurability(RhinestoneArmorMaterial.BASE_DURABILITY)));
    }
    private static Item register(String name, Function<Item.Properties,Item> factory, Item.Properties properties) {
        ResourceKey<Item> key=ResourceKey.create(BuiltInRegistries.ITEM.key(),NeverAlone.id(name));
        return Registry.register(BuiltInRegistries.ITEM,key,factory.apply(properties.setId(key)));
    }
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(e->e.accept(RADIANT_RHINESTONE));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(e->{e.accept(RHINESTONE_TIARA);e.accept(RHINESTONE_CHESTPLATE);e.accept(RHINESTONE_LEGGINGS);e.accept(RHINESTONE_BOOTS);});
    }
}