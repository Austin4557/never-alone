package net.locallupo.whisperingspirits;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
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
    private static final int TICKS_PER_CHECK = 10;
    private static final int MIN_COOLDOWN_AFTER_DESPAWN = 800;
    private static final double MAX_VIEW_DISTANCE = 40.0;
    private static final int MIN_SPAWN_RADIUS = 9;
    private static final int SPAWN_RADIUS_VARIATION = 12;
    private static final Map<UUID, Encounter> ENCOUNTERS = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, String> PLAYER_DIMENSIONS = new HashMap<>();
    private static final SoundEvent[] WHISPERS = new SoundEvent[3];
    private record Encounter(String dimension, Vec3 eyes, Vec3 eyeOffset, int ticksLeft) {}
    private int ticks;
    private net.minecraft.server.MinecraftServer lastServer;

    @Override public void onInitialize() {
        String[] names = {"whisperone", "whispertwo", "whisperthree"};
        for (int i = 0; i < names.length; i++) {
            Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, names[i]);
            WHISPERS[i] = Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
        }
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
                            Vec3 left = pos.subtract(encounter.eyeOffset()), right = pos.add(encounter.eyeOffset());
                            level.sendParticles(player, ParticleTypes.END_ROD, true, false,
                                    left.x, left.y, left.z, 1, 0, 0, 0, 0);
                            level.sendParticles(player, ParticleTypes.END_ROD, true, false,
                                    right.x, right.y, right.z, 1, 0, 0, 0, 0);
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
