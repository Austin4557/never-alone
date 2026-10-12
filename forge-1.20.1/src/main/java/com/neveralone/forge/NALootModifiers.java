package com.neveralone.forge;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Forge 1.20.1 Global Loot Modifier codec registration. */
public final class NALootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> CODECS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, NeverAloneForge.ID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> FORTRESS_CAT_EGGS =
        CODECS.register("fortress_cat_eggs", () -> NAFortressEggLootModifier.CODEC);

    private NALootModifiers() { }
}
