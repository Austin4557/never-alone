package com.neveralone.forge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

/**
 * Adds one of the four named vanilla-cat eggs to approximately 20 percent
 * of newly rolled vanilla Nether Fortress chest inventories.
 *
 * The loot-table-ID predicate comes from the JSON data file, not this class;
 * it prevents this modifier from affecting other loot tables.
 *
 * This uses Forge's stacking Global Loot Modifier system. It does NOT replace
 * the vanilla fortress chest loot table or register new custom spawn egg items.
 */
public final class NAFortressEggLootModifier extends LootModifier {
    public static final Codec<NAFortressEggLootModifier> CODEC =
        RecordCodecBuilder.create(instance -> codecStart(instance)
            .apply(instance, NAFortressEggLootModifier::new));

    private static final String[] NAMES = {"Drako", "Klouse", "Lucy", "Oscar"};

    public NAFortressEggLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // Chance is per chest loot roll: 1/5, with each cat equally likely.
        if (context.getRandom().nextInt(5) == 0) {
            String name = NAMES[context.getRandom().nextInt(NAMES.length)];
            generatedLoot.add(NACatEggs.makeEgg(name));
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
