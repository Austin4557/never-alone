package com.neveralone.forge;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * Non-destructive Forge ambient cue scheduler.
 * Opt-in per player; state resets on disconnect/restart.
 * Uses global server ticks so changing dimensions cannot strand a pending event.
 */
public final class NADirector {
    private static final int DEFAULT_SECONDS = 75;
    private static final int MIN_SECONDS = 45;
    private static final int MAX_SECONDS = 300;
    private static final int FIRST_EVENT_DELAY = 900; // 45 sec

    private static final Map<UUID, Schedule> SCHEDULES = new HashMap<>();

    private static final class Schedule {
        long nextServerTick;
        int intervalSeconds;
        long eventsTriggered;

        Schedule(long now) {
            this.nextServerTick = now + FIRST_EVENT_DELAY;
            this.intervalSeconds = DEFAULT_SECONDS;
            this.eventsTriggered = 0;
        }
    }

    private NADirector() { }

    private static long currentTick(ServerPlayer player) {
        return player.serverLevel().getServer().getTickCount();
    }

    public static boolean enabled(ServerPlayer player) {
        return SCHEDULES.containsKey(player.getUUID());
    }

    public static void setEnabled(ServerPlayer player, boolean on) {
        if (on) {
            SCHEDULES.computeIfAbsent(player.getUUID(), id -> new Schedule(currentTick(player)));
        } else {
            SCHEDULES.remove(player.getUUID());
        }
    }

    /** Sets a fixed interval for opt-in minor events. Does not auto-enable. */
    public static boolean setInterval(ServerPlayer player, int seconds) {
        if (seconds < MIN_SECONDS || seconds > MAX_SECONDS) return false;
        Schedule state = SCHEDULES.get(player.getUUID());
        if (state == null) return false;
        state.intervalSeconds = seconds;
        state.nextServerTick = currentTick(player) + seconds * 20L;
        return true;
    }

    public static String status(ServerPlayer player) {
        Schedule state = SCHEDULES.get(player.getUUID());
        if (state == null) return "off (resets after restart)";
        long secondsLeft = Math.max(0L, (state.nextServerTick - currentTick(player) + 19L) / 20L);
        return "on; interval=" + state.intervalSeconds + "s; next cue in ~"
            + secondsLeft + "s; triggered=" + state.eventsTriggered;
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SCHEDULES.remove(event.getEntity().getUUID());
    }

    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer)) return;
        ServerPlayer player = (ServerPlayer) event.player;
        Schedule state = SCHEDULES.get(player.getUUID());
        if (state == null || player.isSpectator() || !player.isAlive()) return;
        long now = currentTick(player);
        if (now < state.nextServerTick) return;
        // Scares 1-3 are harmless sound/particle cues. No world edits or fake stalker.
        int effect = 1 + player.getRandom().nextInt(3);
        NAScareEffects.trigger(player, effect);
        state.eventsTriggered++;
        state.nextServerTick = now + state.intervalSeconds * 20L;
    }
}
