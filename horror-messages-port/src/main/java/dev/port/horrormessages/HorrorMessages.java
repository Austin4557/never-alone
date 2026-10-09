package dev.port.horrormessages;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;
import static net.minecraft.commands.Commands.literal;

/**
 * Server-side timed horror chat messages. No client rendering or networking required.
 * The text catalog is a compatible recreation, not a claim of verbatim original dialogue.
 */
public final class HorrorMessages implements ModInitializer {
    private static final int MIN_INTERVAL_TICKS = 10 * 60 * 20;
    private static final int MAX_EXTRA_TICKS = 5 * 60 * 20;
    private static final String[] MESSAGES = {
        "Did you hear that?",
        "You are not alone.",
        "Something moved behind you.",
        "Don't look into the darkness for too long.",
        "Someone is watching from far away.",
        "You hear footsteps, but no one is there.",
        "The silence feels wrong.",
        "Something is standing just out of sight.",
        "Did the shadows just change?",
        "It knows where you are.",
        "A faint whisper fades into the distance.",
        "You feel like you are being followed.",
        "The world seems unusually quiet.",
        "You catch a glimpse of movement in the corner of your eye.",
        "The feeling of being watched won't go away.",
        "Something is waiting in the dark."
    };
    private static final Map<UUID, Integer> REMAINING = new HashMap<>();
    private static final Map<UUID, Integer> PREVIOUS = new HashMap<>();
    private MinecraftServer activeServer;

    @Override public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, selection) ->
            dispatcher.register(literal("horrormessages")
                .then(literal("test").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    sendRandom(player);
                    return 1;
                }))
                .then(literal("next").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    int remaining = REMAINING.getOrDefault(player.getUUID(), 0);
                    player.sendSystemMessage(Component.literal("Next horror message in approximately " + ((remaining + 1199) / 1200) + " minute(s)."));
                    return 1;
                }))
                .then(literal("reset").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    REMAINING.put(player.getUUID(), nextInterval(player));
                    player.sendSystemMessage(Component.literal("Horror message timer reset."));
                    return 1;
                }))
            )
        );
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (activeServer != server) {
                REMAINING.clear();
                PREVIOUS.clear();
                activeServer = server;
            }
            Set<UUID> connected = server.getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID).collect(Collectors.toSet());
            REMAINING.keySet().retainAll(connected);
            PREVIOUS.keySet().retainAll(connected);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID id = player.getUUID();
                int remaining = REMAINING.computeIfAbsent(id, ignored -> nextInterval(player)) - 1;
                if (remaining <= 0) {
                    sendRandom(player);
                    remaining = nextInterval(player);
                }
                REMAINING.put(id, remaining);
            }
        });
    }

    private static int nextInterval(ServerPlayer player) {
        return MIN_INTERVAL_TICKS + player.getRandom().nextInt(MAX_EXTRA_TICKS + 1);
    }

    private static void sendRandom(ServerPlayer player) {
        UUID id = player.getUUID();
        int previous = PREVIOUS.getOrDefault(id, -1);
        int selected = player.getRandom().nextInt(MESSAGES.length);
        if (selected == previous) selected = (selected + 1) % MESSAGES.length;
        PREVIOUS.put(id, selected);
        player.sendSystemMessage(Component.literal(MESSAGES[selected]));
    }
}
