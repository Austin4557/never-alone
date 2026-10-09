package dev.port.donothearthem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;
import static net.minecraft.commands.Commands.literal;

/** Independent, original implementation: atmospheric testing prototype. */
public final class DoNotHearThemPrototype implements ModInitializer {
    private enum EncounterKind { CAVE, WINDOW, FOREST, SLEEP }
    private record Encounter(EncounterKind kind, String dimension, Vec3 location, int remainingTicks) {}
    private static final Map<UUID, Encounter> ENCOUNTERS = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Boolean> SLEEP_STATE = new HashMap<>();
    private MinecraftServer previousServer;
    private int tick;

    @Override public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, selection) ->
            dispatcher.register(literal("dnht")
                .then(literal("test")
                    .then(literal("cave").executes(c -> spawn(c.getSource().getPlayerOrException(), EncounterKind.CAVE)))
                    .then(literal("window").executes(c -> spawn(c.getSource().getPlayerOrException(), EncounterKind.WINDOW)))
                    .then(literal("forest").executes(c -> spawn(c.getSource().getPlayerOrException(), EncounterKind.FOREST)))
                    .then(literal("sleep").executes(c -> spawn(c.getSource().getPlayerOrException(), EncounterKind.SLEEP))))
                .then(literal("clear").executes(c -> clear(c.getSource().getPlayerOrException())))
                .then(literal("debug").executes(c -> debug(c.getSource().getPlayerOrException())))
            )
        );
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (previousServer != server) {
                ENCOUNTERS.clear(); COOLDOWNS.clear(); SLEEP_STATE.clear();
                previousServer = server; tick = 0;
            }
            if (++tick % 10 != 0) return;
            Set<UUID> online = server.getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID).collect(Collectors.toSet());
            ENCOUNTERS.keySet().retainAll(online);
            COOLDOWNS.keySet().retainAll(online);
            SLEEP_STATE.keySet().retainAll(online);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.level() instanceof ServerLevel level)) continue;
                UUID id = player.getUUID();
                if (!player.isAlive() || player.isSpectator()) {
                    ENCOUNTERS.remove(id); SLEEP_STATE.remove(id); continue;
                }
                boolean sleeping = player.isSleeping();
                Boolean priorSleep = SLEEP_STATE.put(id, sleeping);
                if (priorSleep != null && priorSleep && !sleeping && level.getRandom().nextInt(4) == 0) {
                    spawn(player, EncounterKind.SLEEP);
                }
                Encounter encounter = ENCOUNTERS.get(id);
                if (encounter != null) {
                    if (!encounter.dimension.equals(level.dimension().identifier().toString())
                            || encounter.remainingTicks <= 0) {
                        ENCOUNTERS.remove(id); continue;
                    }
                    if (tick % 20 == 0) render(player, level, encounter);
                    ENCOUNTERS.put(id, new Encounter(encounter.kind, encounter.dimension,
                            encounter.location, encounter.remainingTicks-10));
                    continue;
                }
                int cooldown = COOLDOWNS.getOrDefault(id, 0);
                if (cooldown > 0) { COOLDOWNS.put(id, Math.max(0,cooldown-10)); continue; }
                if (level.getRandom().nextInt(600) != 0) continue;
                EncounterKind chosen;
                if (player.getBlockY() < 55 && level.getMaxLocalRawBrightness(player.blockPosition()) <= 7) chosen=EncounterKind.CAVE;
                else if (level.isNight()) chosen=EncounterKind.FOREST;
                else continue;
                spawn(player, chosen);
            }
        });
    }

    private static int spawn(ServerPlayer player, EncounterKind kind) {
        if (!(player.level() instanceof ServerLevel level)) return 0;
        Vec3 look=player.getLookAngle();
        Vec3 lateral=new Vec3(-look.z,0,look.x);
        if(lateral.lengthSqr()<.001) lateral=new Vec3(1,0,0);
        lateral=lateral.normalize();
        Vec3 point=player.position().add(lateral.scale(kind==EncounterKind.SLEEP ? 3 : 8));
        BlockPos feet=locate(level, BlockPos.containing(point));
        if (feet == null) {
            player.sendSystemMessage(Component.literal("[DNHT test] No loaded, unobstructed surface nearby."));
            return 0;
        }
        Vec3 location=new Vec3(feet.getX()+.5,feet.getY()+1.4,feet.getZ()+.5);
        ENCOUNTERS.put(player.getUUID(),new Encounter(kind,level.dimension().identifier().toString(),location,280));
        COOLDOWNS.put(player.getUUID(),2400);
        player.sendSystemMessage(Component.literal("[DNHT test] " + kind.name().toLowerCase() + " encounter triggered."));
        sendSound(player,level,location);
        render(player,level,ENCOUNTERS.get(player.getUUID()));
        return 1;
    }

    private static BlockPos locate(ServerLevel level, BlockPos origin) {
        for (int dy=4;dy>=-7;dy--) {
            BlockPos pos=origin.offset(0,dy,0);
            if (!level.isInWorldBounds(pos) || !level.isInWorldBounds(pos.above())) continue;
            if (!level.hasChunkAt(pos) || !level.hasChunkAt(pos.below())) continue;
            if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP))
                return pos;
        }
        return null;
    }
    private static void render(ServerPlayer player, ServerLevel level, Encounter encounter) {
        Vec3 p=encounter.location;
        switch(encounter.kind) {
            case CAVE -> {
                level.sendParticles(player,ParticleTypes.SMOKE,true,false,p.x,p.y,p.z,4,.2,.5,.2,.001);
                level.sendParticles(player,ParticleTypes.SOUL_FIRE_FLAME,true,false,p.x,p.y+0.3,p.z,1,0,0,0,0);
            }
            case WINDOW -> {
                level.sendParticles(player,ParticleTypes.END_ROD,true,false,p.x-.15,p.y+.2,p.z,1,0,0,0,0);
                level.sendParticles(player,ParticleTypes.END_ROD,true,false,p.x+.15,p.y+.2,p.z,1,0,0,0,0);
            }
            case FOREST -> {
                level.sendParticles(player,ParticleTypes.ASH,true,false,p.x,p.y,p.z,5,.4,.8,.4,.001);
            }
            case SLEEP -> {
                level.sendParticles(player,ParticleTypes.SOUL,true,false,p.x,p.y,p.z,3,.3,.3,.3,.001);
            }
        }
    }
    private static void sendSound(ServerPlayer player,ServerLevel level,Vec3 location) {
        player.connection.send(new ClientboundSoundPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.CAVE_AMBIENT),
            SoundSource.AMBIENT,location.x,location.y,location.z,
            0.75f,0.65f+level.getRandom().nextFloat()*.3f,level.getRandom().nextLong()));
    }
    private static int clear(ServerPlayer player) {
        ENCOUNTERS.remove(player.getUUID());
        COOLDOWNS.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("[DNHT test] Encounter and cooldown cleared."));
        return 1;
    }
    private static int debug(ServerPlayer player) {
        Encounter encounter=ENCOUNTERS.get(player.getUUID());
        int seconds=COOLDOWNS.getOrDefault(player.getUUID(),0)/20;
        player.sendSystemMessage(Component.literal(encounter == null
            ? "[DNHT test] No active encounter. Cooldown: "+seconds+"s."
            : "[DNHT test] "+encounter.kind+" at "+encounter.location+" for "+encounter.remainingTicks/20+"s."));
        return 1;
    }
}
