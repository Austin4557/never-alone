package com.neveralone;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;

public final class NeverAlone implements ModInitializer {
    public static final String MOD_ID = "never_alone";

    public static final SoundEvent SCARE_1 = registerSound("scare_1");
    public static final SoundEvent SCARE_2 = registerSound("scare_2");
    public static final SoundEvent SCARE_3 = registerSound("scare_3");
    public static final SoundEvent SCARE_4 = registerSound("scare_4");

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static SoundEvent registerSound(String name) {
        Identifier id = id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    @Override
    public void onInitialize() {
    }
}
