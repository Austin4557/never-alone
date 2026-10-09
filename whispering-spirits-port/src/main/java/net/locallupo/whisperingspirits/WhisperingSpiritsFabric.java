package net.locallupo.whisperingspirits;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import static net.minecraft.commands.Commands.literal;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

public final class WhisperingSpiritsFabric implements ModInitializer {
    private static final String MOD_ID = "whispering_spirits";
    public static final SimpleParticleType WATCHER_EYES_PARTICLE = new SimpleParticleType(false);
    private static final int TICKS_PER_CHECK = 10;
    private static final int MIN_COOLDOWN_AFTER_DESPAWN = 800;
    private static final double MAX_VIEW_DISTANCE = 40.0;
    private static final int MIN_SPAWN_RADIUS = 9;
    private static final int SPAWN_RADIUS_VARIATION = 12;
    private static final Map<UUID, Encounter> ENCOUNTERS = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, String> PLAYER_DIMENSIONS = new HashMap<>();
    private static final SoundEvent[] WHISPERS = new SoundEvent[3];
    private static final int FORCED_DURATION = 1200;
    private record Encounter(String dimension, Vec3 eyes, Vec3 eyeOffset, int ticksLeft) {}
    private int ticks;
    private net.minecraft.server.MinecraftServer lastServer;

    @Override public void onInitialize() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "watcher_eyes"), WATCHER_EYES_PARTICLE);
        String[] names = {"whisperone", "whispertwo", "whisperthree"};
        for (int i = 0; i < names.length; i++) {
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, names[i]);
            WHISPERS[i] = Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, selection) ->
            dispatcher.register(literal("whisperingspirits")
                .then(literal("spawn").executes(ctx -> spawnCommand(ctx.getSource().getPlayerOrException())))
                .then(literal("whisper").executes(ctx -> whisperCommand(ctx.getSource().getPlayerOrException())))
                .then(literal("clear").executes(ctx -> clearCommand(ctx.getSource().getPlayerOrException())))
                .then(literal("debug").executes(ctx -> debugCommand(ctx.getSource().getPlayerOrException())))
            )
        );
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (lastServer != server) {
                ENCOUNTERS.clear(); COOLDOWNS.clear(); PLAYER_DIMENSIONS.clear();
                ticks = 0; lastServer = server;
            }
            if (++ticks % TICKS_PER_CHECK != 0) return;
            Set<UUID> online = server.getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).collect(Collectors.toSet());
            ENCOUNTERS.keySet().retainAll(online);
            COOLDOWNS.keySet().retainAll(online);
            PLAYER_DIMENSIONS.keySet().retainAll(online);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.level() instanceof ServerLevel level)) continue;
                UUID id = player.getUUID();
                if (player.isSpectator() || !player.isAlive()) {
                    ENCOUNTERS.remove(id); COOLDOWNS.remove(id); PLAYER_DIMENSIONS.remove(id); continue;
                }
                String dimension = level.dimension().identifier().toString();
                String priorDimension = PLAYER_DIMENSIONS.put(id, dimension);
                if (priorDimension != null && !priorDimension.equals(dimension)) {
                    ENCOUNTERS.remove(id); COOLDOWNS.put(id, 200); continue;
                }
                Encounter encounter = ENCOUNTERS.get(id);
                if (encounter != null) {
                    if (!encounter.dimension().equals(dimension) || encounter.ticksLeft() <= 0) {
                        ENCOUNTERS.remove(id);
                        COOLDOWNS.merge(id, MIN_COOLDOWN_AFTER_DESPAWN, Math::max);
                        continue;
                    }
                    Vec3 eyePosition = player.getEyePosition();
                    Vec3 toSpirit = encounter.eyes().subtract(eyePosition);
                    boolean seen = toSpirit.lengthSqr() < MAX_VIEW_DISTANCE * MAX_VIEW_DISTANCE
                            && toSpirit.lengthSqr() > 0.001
                            && player.getLookAngle().normalize().dot(toSpirit.normalize()) > 0.975
                            && hasClearSight(level, player, encounter.eyes());
                    if (seen) {
                        SoundEvent sound = WHISPERS[level.getRandom().nextInt(WHISPERS.length)];
                        player.connection.send(new ClientboundSoundPacket(
                                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.AMBIENT,
                                encounter.eyes().x, encounter.eyes().y, encounter.eyes().z,
                                0.75f, 0.85f + level.getRandom().nextFloat() * 0.3f, level.getRandom().nextLong()));
                        ENCOUNTERS.remove(id);
                        COOLDOWNS.merge(id, MIN_COOLDOWN_AFTER_DESPAWN, Math::max);
                    } else {
                        Vec3 pos = encounter.eyes();
                        if (level.getRandom().nextInt(5) != 0) {
                            level.sendParticles(player, WATCHER_EYES_PARTICLE, true, false,
                                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
                        }
                        ENCOUNTERS.put(id, new Encounter(dimension, pos, encounter.eyeOffset(), encounter.ticksLeft() - TICKS_PER_CHECK));
                    }
                    continue;
                }
                int remaining = COOLDOWNS.getOrDefault(id, 0);
                if (remaining > 0) { COOLDOWNS.put(id, Math.max(0, remaining - TICKS_PER_CHECK)); continue; }
                if (level.getRandom().nextInt(120) != 0) continue;
                double angle = level.getRandom().nextDouble() * Math.PI * 2;
                double radius = MIN_SPAWN_RADIUS + level.getRandom().nextDouble() * SPAWN_RADIUS_VARIATION;
                int x = (int)Math.floor(player.getX() + Math.cos(angle)*radius);
                int z = (int)Math.floor(player.getZ() + Math.sin(angle)*radius);
                BlockPos feet = locateDarkSurface(level, new BlockPos(x, player.getBlockY(), z));
                if (feet == null) continue;
                Vec3 position = new Vec3(feet.getX() + .5, feet.getY()+1.6, feet.getZ()+.5);
                if (!hasClearSight(level, player, position)) continue;
                Vec3 direction = position.subtract(player.getEyePosition());
                if (direction.lengthSqr() > .001 && player.getLookAngle().normalize().dot(direction.normalize()) > .975) continue;
                double horizontal = Math.hypot(direction.x, direction.z);
                if (horizontal < .01) continue;
                Vec3 eyeOffset = new Vec3(-direction.z / horizontal * .18, 0, direction.x / horizontal * .18);
                ENCOUNTERS.put(id, new Encounter(dimension, position, eyeOffset, 300 + level.getRandom().nextInt(300)));
                COOLDOWNS.put(id, 800 + level.getRandom().nextInt(1200));
            }
        });
    }

    private static int spawnCommand(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 0.001) horizontal = new Vec3(0, 0, 1);
        horizontal = horizontal.normalize();
        // Spawn to the player's side to avoid triggering look detection immediately.
        Vec3 sideways = new Vec3(-horizontal.z, 0, horizontal.x);
        Vec3 approximate = player.position().add(sideways.scale(9));
        BlockPos surface = locateSurfaceForCommand(level, BlockPos.containing(approximate));
        if (surface == null) {
            player.sendSystemMessage(Component.literal("No safe spawn location nearby. Try open ground."));
            return 0;
        }
        Vec3 eyes = new Vec3(surface.getX() + 0.5, surface.getY() + 1.6, surface.getZ() + 0.5);
        Vec3 toward = player.position().subtract(eyes);
        double len = Math.hypot(toward.x, toward.z);
        Vec3 offset = len < 0.001 ? new Vec3(.18,0,0) : new Vec3(-toward.z / len * .18, 0, toward.x / len * .18);
        ENCOUNTERS.put(player.getUUID(), new Encounter(level.dimension().identifier().toString(), eyes, offset, FORCED_DURATION));
        COOLDOWNS.put(player.getUUID(), MIN_COOLDOWN_AFTER_DESPAWN);
        PLAYER_DIMENSIONS.put(player.getUUID(), level.dimension().identifier().toString());
        player.sendSystemMessage(Component.literal("Spirit spawned nearby. Turn to your side and look for glowing eyes."));
        return 1;
    }
    private static BlockPos locateSurfaceForCommand(ServerLevel level, BlockPos around) {
        for (int offset = 4; offset >= -7; offset--) {
            BlockPos feet = around.offset(0, offset, 0);
            if (!level.isInWorldBounds(feet) || !level.hasChunkAt(feet) || !level.hasChunkAt(feet.below())) continue;
            if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
                && level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),net.minecraft.core.Direction.UP)) return feet;
        }
        return null;
    }
    private static int whisperCommand(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        SoundEvent sound = WHISPERS[level.getRandom().nextInt(WHISPERS.length)];
        Vec3 pos = player.position();
        player.connection.send(new ClientboundSoundPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.AMBIENT,
            pos.x, pos.y, pos.z, 1.0f, 1.0f, level.getRandom().nextLong()));
        player.sendSystemMessage(Component.literal("Playing a whisper sound."));
        return 1;
    }
    private static int clearCommand(ServerPlayer player) {
        boolean existed = ENCOUNTERS.remove(player.getUUID()) != null;
        COOLDOWNS.remove(player.getUUID());
        player.sendSystemMessage(Component.literal(existed ? "Spirit cleared; cooldown reset." : "No active spirit; cooldown reset."));
        return 1;
    }
    private static int debugCommand(ServerPlayer player) {
        Encounter active = ENCOUNTERS.get(player.getUUID());
        int cooldown = COOLDOWNS.getOrDefault(player.getUUID(), 0);
        player.sendSystemMessage(Component.literal(active == null
            ? "Whispering Spirits: no encounter; cooldown " + cooldown/20 + "s."
            : "Whispering Spirits: active at " + active.eyes() + "; " + active.ticksLeft()/20 + "s remaining."));
        return 1;
    }

    private static BlockPos locateDarkSurface(ServerLevel level, BlockPos around) {
        for (int offset=3;offset>=-5;offset--) {
            BlockPos feet=around.offset(0,offset,0);
            if (!level.isInWorldBounds(feet) || !level.isInWorldBounds(feet.above()) || !level.isInWorldBounds(feet.below())) continue;
            if (!level.hasChunkAt(feet) || !level.hasChunkAt(feet.above()) || !level.hasChunkAt(feet.below())) continue;
            if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) continue;
            if (!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),net.minecraft.core.Direction.UP)) continue;
            if (level.getMaxLocalRawBrightness(feet.above()) > 7) continue;
            return feet;
        }
        return null;
    }
    private static boolean hasClearSight(ServerLevel level, ServerPlayer player, Vec3 target) {
        return level.clip(new ClipContext(player.getEyePosition(),target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType() == HitResult.Type.MISS;
    }
}
