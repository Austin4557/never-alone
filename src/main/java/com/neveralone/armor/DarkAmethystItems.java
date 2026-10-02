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

public final class DarkAmethystItems {
    private DarkAmethystItems() {}

    public static final Item DARK_AMETHYST_CRYSTAL = register("dark_amethyst_crystal", Item::new,
            new Item.Properties());

    public static final Item DARK_AMETHYST_HELMET = armor("dark_amethyst_helmet", ArmorType.HELMET);
    public static final Item DARK_AMETHYST_CHESTPLATE = armor("dark_amethyst_chestplate", ArmorType.CHESTPLATE);
    public static final Item DARK_AMETHYST_LEGGINGS = armor("dark_amethyst_leggings", ArmorType.LEGGINGS);
    public static final Item DARK_AMETHYST_BOOTS = armor("dark_amethyst_boots", ArmorType.BOOTS);

    private static Item armor(String name, ArmorType type) {
        return register(name, Item::new,
                new Item.Properties()
                        .humanoidArmor(DarkAmethystArmorMaterial.INSTANCE, type)
                        .durability(type.getDurability(DarkAmethystArmorMaterial.BASE_DURABILITY)));
    }

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(BuiltInRegistries.ITEM.key(), NeverAlone.id(name));
        Item item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(entries -> entries.accept(DARK_AMETHYST_CRYSTAL));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register(entries -> {
                    entries.accept(DARK_AMETHYST_HELMET);
                    entries.accept(DARK_AMETHYST_CHESTPLATE);
                    entries.accept(DARK_AMETHYST_LEGGINGS);
                    entries.accept(DARK_AMETHYST_BOOTS);
                });
    }
}
