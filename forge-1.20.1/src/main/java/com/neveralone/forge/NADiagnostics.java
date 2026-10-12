package com.neveralone.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Runtime smoke tests for item registry and egg NBT after a *real* Forge build.
 * These tests cannot establish whether resource-pack models or chest loot render.
 */
public final class NADiagnostics {
    private static final String[] CATS = {"Drako", "Klouse", "Lucy", "Oscar"};

    private NADiagnostics() { }

    private static void say(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[Never Alone check] " + message), false);
    }

    public static int checkArmor(CommandSourceStack source) {
        List<String> issues = new ArrayList<>();
        int pieces = 0;
        for (Map.Entry<String, Map<String, RegistryObject<Item>>> set : NAItems.ARMOR.entrySet()) {
            String setName = set.getKey();
            if (set.getValue().size() != 4) issues.add(setName + " needs four armor slots");
            for (Map.Entry<String, RegistryObject<Item>> entry : set.getValue().entrySet()) {
                RegistryObject<Item> ref = entry.getValue();
                if (!ref.isPresent()) {
                    issues.add("Missing " + setName + "/" + entry.getKey());
                    continue;
                }
                pieces++;
                if (!(ref.get() instanceof ArmorItem)) issues.add("Not armor: " + setName + "/" + entry.getKey());
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(ref.get());
                if (id == null || !NeverAloneForge.ID.equals(id.getNamespace())) {
                    issues.add("Wrong registered item ID: " + setName + "/" + entry.getKey());
                }
            }
        }
        for (Map.Entry<String, RegistryObject<Item>> ingredient : NAItems.MATERIALS.entrySet()) {
            if (!ingredient.getValue().isPresent()) issues.add("Missing ingredient " + ingredient.getKey());
        }
        if (pieces != 12) issues.add("Expected 12 armor items, found " + pieces);
        if (NAItems.MATERIALS.size() != 3) issues.add("Expected three armor materials");
        return result(source, "Armor registry", issues);
    }

    public static int checkEggs(CommandSourceStack source) {
        List<String> issues = new ArrayList<>();
        int count = 0;
        for (String name : CATS) {
            ItemStack egg = NACatEggs.makeEgg(name);
            CompoundTag tag = egg.getTag();
            if (egg.getItem() != Items.CAT_SPAWN_EGG || tag == null) {
                issues.add(name + " has no vanilla cat spawn-egg NBT");
                continue;
            }
            count++;
            if (tag.getInt("CustomModelData") != 1201551 + (count-1)) {
                issues.add(name + " mismatched resource-pack icon ID");
            }
            CompoundTag entity = tag.getCompound("EntityTag");
            if (!"minecraft:cat".equals(entity.getString("id"))) {
                issues.add(name + " has wrong entity ID");
            }
            if (!entity.getString("CustomName").contains(name)) {
                issues.add(name + " has no matching visible name");
            }
            if (!entity.contains("variant")) issues.add(name + " has no fallback variant");
        }
        if (count != 4) issues.add("Expected four cat egg stacks, found " + count);
        return result(source, "Named cat egg NBT", issues);
    }

    public static int checkLoot(CommandSourceStack source) {
        List<String> issues = new ArrayList<>();
        if (!NALootModifiers.FORTRESS_CAT_EGGS.isPresent()) {
            issues.add("Nether Fortress loot modifier codec is not registered");
        }
        int status = result(source, "Fortress loot codec registry", issues);
        say(source, "Codec presence does not prove the chest loot modifier JSON is loaded. Test a fresh fortress chest separately.");
        return status;
    }

    public static int checkAll(CommandSourceStack source) {
        int armor = checkArmor(source);
        int eggs = checkEggs(source);
        int loot = checkLoot(source);
        return (armor == 1 && eggs == 1 && loot == 1) ? 1 : 0;
    }

    private static int result(CommandSourceStack source, String name, List<String> errors) {
        if (errors.isEmpty()) {
            say(source, name + ": PASS (registry/NBT only)");
            return 1;
        }
        say(source, name + ": FAIL: " + String.join("; ", errors));
        return 0;
    }
}
