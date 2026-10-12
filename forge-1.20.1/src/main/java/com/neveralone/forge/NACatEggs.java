package com.neveralone.forge;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Makes *vanilla* 1.20.1 cat spawn egg items with preset entity NBT.
 *
 * We deliberately do NOT register ForgeSpawnEggItem here. The earlier
 * experimental stub-compiled Forge JAR crashed in Forge common setup.
 * These eggs use the pre-existing vanilla item registry and can be created
 * by server-side commands without adding an entity/item registration.
 *
 * Named cat textures are provided by the independently tested ETF/EMF
 * v5 resource pack; these NBT values are vanilla fallback coat variants.
 */
public final class NACatEggs {
    private static final Map<String, String> VARIANTS = new LinkedHashMap<>();
    private static final Map<String, Integer> ICON_IDS = new LinkedHashMap<>();
    static {
        VARIANTS.put("Drako", "minecraft:calico");
        VARIANTS.put("Klouse", "minecraft:tabby");
        VARIANTS.put("Lucy", "minecraft:british_shorthair");
        VARIANTS.put("Oscar", "minecraft:red");
        // Existing, tested Cat Ears v5 resource-pack model predicates.
        ICON_IDS.put("Drako", 1201551);
        ICON_IDS.put("Klouse", 1201552);
        ICON_IDS.put("Lucy", 1201553);
        ICON_IDS.put("Oscar", 1201554);
    }

    public static ItemStack makeEgg(String name) {
        String variant = VARIANTS.get(name);
        if (variant == null) {
            throw new IllegalArgumentException("Unknown named cat: " + name);
        }
        ItemStack stack = new ItemStack(Items.CAT_SPAWN_EGG);
        stack.setHoverName(Component.literal("summon " + name.toLowerCase(Locale.ROOT)));
        // Model override lives in the separate v5 resource pack; no resource
        // pack changes or duplicate icon art needed in the Forge mod.
        stack.getOrCreateTag().putInt("CustomModelData", ICON_IDS.get(name));

        CompoundTag entityTag = stack.getOrCreateTagElement("EntityTag");
        entityTag.putString("id", "minecraft:cat");
        entityTag.putString("CustomName", Component.Serializer.toJson(Component.literal(name)));
        entityTag.putBoolean("CustomNameVisible", true);
        entityTag.putString("variant", variant);
        entityTag.putBoolean("PersistenceRequired", true);
        return stack;
    }

    public static int give(ServerPlayer player, String name) {
        ItemStack stack = makeEgg(name);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        return 1;
    }

    public static int giveAll(ServerPlayer player) {
        for (String name : VARIANTS.keySet()) {
            give(player, name);
        }
        return VARIANTS.size();
    }

    private NACatEggs() { }
}
