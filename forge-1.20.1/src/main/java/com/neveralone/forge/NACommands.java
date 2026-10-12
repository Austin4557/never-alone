package com.neveralone.forge;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class NACommands {
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("na").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("status").executes(c -> {
            String director = c.getSource().getEntity() instanceof ServerPlayer
                && NADirector.enabled((ServerPlayer) c.getSource().getEntity()) ? "on" : "off";
            info(c.getSource(), "Forge Stage 7 test: armor/equip, eggs, scares, timed ambient cues, and fortress loot. Director=" + director + ". Stalker and full Forge port are still pending.");
            return 1;
        }));
        LiteralArgumentBuilder<CommandSourceStack> armor = Commands.literal("armor");
        for (String set : NAItems.ARMOR.keySet()) {
            armor.then(Commands.literal(set).executes(c -> giveArmor(c.getSource(), set)));
        }
        // Direct equip tests simplify verifying slot assignments/armor layers.
        LiteralArgumentBuilder<CommandSourceStack> equip = Commands.literal("equip");
        for (String set : NAItems.ARMOR.keySet()) {
            equip.then(Commands.literal(set).executes(c -> equipArmor(c.getSource(), set)));
        }
        armor.then(equip);
        armor.then(Commands.literal("equipped").executes(c -> armorEquipped(c.getSource())));
        root.then(armor);

        LiteralArgumentBuilder<CommandSourceStack> cat = Commands.literal("cat");
        for (String name : new String[]{"Drako", "Klouse", "Lucy", "Oscar"}) {
            cat.then(Commands.literal(name.toLowerCase(java.util.Locale.ROOT))
                .executes(c -> spawnCat(c.getSource(), name)));
        }
        root.then(cat);

        // Give four NBT-configured vanilla cat spawn eggs, without registering
        // any new Forge item types. The independently working cat resource pack
        // uses the CustomName field to choose each cat's texture.
        LiteralArgumentBuilder<CommandSourceStack> egg = Commands.literal("egg");
        for (String name : new String[]{"Drako", "Klouse", "Lucy", "Oscar"}) {
            egg.then(Commands.literal(name.toLowerCase(java.util.Locale.ROOT))
                .executes(c -> giveEgg(c.getSource(), name)));
        }
        root.then(egg);
        root.then(Commands.literal("eggs").executes(c -> giveAllEggs(c.getSource())));
        root.then(Commands.literal("help").executes(c -> {
            info(c.getSource(), "Commands: status, help, armor <set>, armor equip <set>, armor equipped, cat <name>, egg <name>, eggs, test <door|torch|footsteps|stalker>, scare <1..8>, director <on|off|status|interval 45..300>, check <armor|eggs|loot>.");
            return 1;
        }));

        // Test versions of the familiar Never Alone ambient cues.
        LiteralArgumentBuilder<CommandSourceStack> test = Commands.literal("test");
        test.then(Commands.literal("door").executes(c -> scare(c.getSource(), 2)));
        test.then(Commands.literal("torch").executes(c -> scare(c.getSource(), 3)));
        test.then(Commands.literal("footsteps").executes(c -> scare(c.getSource(), 1)));
        test.then(Commands.literal("stalker").executes(c -> {
            info(c.getSource(), "Real stalker entity is not yet implemented in Forge Stage 6. No substitute creature was spawned.");
            return 0;
        }));
        root.then(test);

        root.then(Commands.literal("scare")
            .then(Commands.argument("level", IntegerArgumentType.integer(1, 8))
                .executes(c -> scare(c.getSource(), IntegerArgumentType.getInteger(c, "level")))));

        LiteralArgumentBuilder<CommandSourceStack> director = Commands.literal("director");
        director.then(Commands.literal("on").executes(c -> toggleDirector(c.getSource(), true)));
        director.then(Commands.literal("off").executes(c -> toggleDirector(c.getSource(), false)));
        director.then(Commands.literal("status").executes(c -> {
            ServerPlayer player = c.getSource().getPlayerOrException();
            info(c.getSource(), "Ambient director: " + NADirector.status(player));
            return 1;
        }));
        director.then(Commands.literal("interval")
            .then(Commands.argument("seconds", IntegerArgumentType.integer(45, 300))
                .executes(c -> directorInterval(c.getSource(),
                    IntegerArgumentType.getInteger(c, "seconds")))));
        root.then(director);

        LiteralArgumentBuilder<CommandSourceStack> check = Commands.literal("check");
        check.executes(c -> NADiagnostics.checkAll(c.getSource()));
        check.then(Commands.literal("armor").executes(c -> NADiagnostics.checkArmor(c.getSource())));
        check.then(Commands.literal("eggs").executes(c -> NADiagnostics.checkEggs(c.getSource())));
        check.then(Commands.literal("loot").executes(c -> NADiagnostics.checkLoot(c.getSource())));
        root.then(check);
        event.getDispatcher().register(root);
    }

    private static int toggleDirector(CommandSourceStack source, boolean enable)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        NADirector.setEnabled(player, enable);
        info(source, "Ambient director " + (enable ? "enabled (minor events only)" : "disabled")
            + ". " + NADirector.status(player));
        return 1;
    }

    private static int directorInterval(CommandSourceStack source, int seconds)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!NADirector.setInterval(player, seconds)) {
            info(source, "Enable the director first with /na director on. Allowed interval: 45..300 seconds.");
            return 0;
        }
        info(source, "Ambient director interval set to " + seconds + "s. " + NADirector.status(player));
        return 1;
    }

    private static int scare(CommandSourceStack source, int id)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int result = NAScareEffects.trigger(player, id);
        info(source, "Triggered non-destructive scare test " + id + ".");
        return result;
    }

    private static void info(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal("[Never Alone] " + message), false);
    }

    private static int giveArmor(CommandSourceStack source, String set)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        for (var item : NAItems.ARMOR.get(set).values()) {
            ItemStack stack = new ItemStack(item.get());
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        info(source, "Gave " + set + " armor (4 pieces).");
        return 1;
    }

    /**
     * Installs all four armor pieces into the intended Minecraft equipment slots.
     * Important: prior armor goes back into the inventory or drops if full;
     * no previous armor is silently destroyed.
     */
    private static int equipArmor(CommandSourceStack source, String set)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var pieces = NAItems.ARMOR.get(set);
        if (pieces == null) return 0;
        equipSlot(player, EquipmentSlot.HEAD, pieces.get("head").get());
        equipSlot(player, EquipmentSlot.CHEST, pieces.get("chest").get());
        equipSlot(player, EquipmentSlot.LEGS, pieces.get("legs").get());
        equipSlot(player, EquipmentSlot.FEET, pieces.get("feet").get());
        info(source, "Equipped all four pieces of " + set + ". Previously worn pieces returned to inventory or were dropped if full.");
        return 1;
    }

    private static void equipSlot(ServerPlayer player, EquipmentSlot slot, net.minecraft.world.item.Item item) {
        ItemStack displaced = player.getItemBySlot(slot).copy();
        player.setItemSlot(slot, new ItemStack(item));
        if (!displaced.isEmpty() && !player.getInventory().add(displaced)) {
            player.drop(displaced, false);
        }
    }

    private static int armorEquipped(CommandSourceStack source)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        StringBuilder status = new StringBuilder("Worn armor: ");
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            if (i > 0) status.append(" | ");
            ItemStack item = player.getItemBySlot(slots[i]);
            status.append(slots[i].getName()).append("=");
            status.append(item.isEmpty() ? "empty" : item.getHoverName().getString());
            if (!item.isEmpty() && item.isDamageableItem()) {
                status.append(" [").append(item.getMaxDamage() - item.getDamageValue())
                    .append("/").append(item.getMaxDamage()).append("]");
            }
        }
        info(source, status.toString());
        return 1;
    }

    private static int giveEgg(CommandSourceStack source, String name)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        NACatEggs.give(player, name);
        info(source, "Gave summon " + name.toLowerCase(java.util.Locale.ROOT) + " (named vanilla cat spawn egg).");
        return 1;
    }

    private static int giveAllEggs(CommandSourceStack source)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int count = NACatEggs.giveAll(player);
        info(source, "Gave " + count + " named vanilla cat spawn eggs.");
        return count;
    }

    private static int spawnCat(CommandSourceStack source, String name)
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Cat cat = EntityType.CAT.create(source.getLevel());
        if (cat == null) {
            info(source, "Cat entity creation failed.");
            return 0;
        }
        cat.setCustomName(Component.literal(name));
        cat.setCustomNameVisible(true);
        cat.setPersistenceRequired();
        cat.moveTo(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYRot(), 0);
        source.getLevel().addFreshEntity(cat);
        info(source, "Summoned " + name + " (named vanilla cat for ETF/EMF).");
        return 1;
    }

    private NACommands() { }
}
