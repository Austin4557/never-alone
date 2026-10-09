package dev.port.donothearthem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.math.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;
import static net.minecraft.commands.Commands.literal;

/** Independent, original implementation: atmospheric testing prototype. */
public final class DoNotHearThemPrototype implements ModInitializer {
    public static final SimpleParticleType CAVE_SPRITE = FabricParticleTypes.simple();
    public static final SimpleParticleType FOREST_SPRITE = FabricParticleTypes.simple();
    public static final SimpleParticleType WINDOW_SPRITE = FabricParticleTypes.simple();
    public static final SimpleParticleType SLEEP_SPRITE = FabricParticleTypes.simple();
    private enum EncounterKind { CAVE, WINDOW, FOREST, SLEEP }
    private record Encounter(EncounterKind kind, String dimension, Vec3 location, int remainingTicks, int observedTicks) {}
    private static final Map<UUID, Encounter> ENCOUNTERS = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Boolean> SLEEP_STATE = new HashMap<>();
    // Solid vanilla BlockDisplay parts supplement the high-detail sprite with true spatial depth.
    private static final Map<UUID, List<Display.BlockDisplay>> BODY_PARTS = new HashMap<>();
    private MinecraftServer previousServer;
    private int tick;

    @Override public void onInitialize() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("donothearthemprototype", "stalker_cave"), CAVE_SPRITE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("donothearthemprototype", "stalker_forest"), FOREST_SPRITE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("donothearthemprototype", "stalker_window"), WINDOW_SPRITE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("donothearthemprototype", "stalker_sleep"), SLEEP_SPRITE);
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
                for (UUID id : new ArrayList<>(BODY_PARTS.keySet())) clearBody(id);
                ENCOUNTERS.clear(); COOLDOWNS.clear(); SLEEP_STATE.clear();
                previousServer = server; tick = 0;
            }
            if (++tick % 10 != 0) return;
            Set<UUID> online = server.getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID).collect(Collectors.toSet());
            ENCOUNTERS.keySet().retainAll(online);
            COOLDOWNS.keySet().retainAll(online);
            SLEEP_STATE.keySet().retainAll(online);
            for (UUID id : new ArrayList<>(BODY_PARTS.keySet())) if (!online.contains(id)) clearBody(id);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.level() instanceof ServerLevel level)) continue;
                UUID id = player.getUUID();
                if (!player.isAlive() || player.isSpectator()) {
                    ENCOUNTERS.remove(id); clearBody(id); SLEEP_STATE.remove(id); continue;
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
                        ENCOUNTERS.remove(id); clearBody(id); continue;
                    }
                    Vec3 toward = encounter.location.subtract(player.getEyePosition());
                    boolean observed = toward.lengthSqr() < 900 && toward.lengthSqr() > .001
                        && player.getLookAngle().normalize().dot(toward.normalize()) > .955
                        && level.clip(new ClipContext(player.getEyePosition(), encounter.location,
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
                    int observedTicks = observed ? encounter.observedTicks + 10 : Math.max(0, encounter.observedTicks - 10);
                    int elapsedTicks = Math.max(0, 500 - encounter.remainingTicks);
                    Vec3 nextLocation = encounter.location;
                    int sightThreshold = switch (encounter.kind) {
                        case CAVE -> 50;     // Keeps advancing until faced down.
                        case FOREST -> 20;   // Vanishes almost immediately when spotted.
                        case WINDOW -> 70;   // Holds its stare before slipping away.
                        case SLEEP -> 40;    // Floats, then dissolves.
                    };

                    if (encounter.kind == EncounterKind.CAVE && !observed) {
                        Vec3 move = player.position().subtract(encounter.location);
                        if (move.lengthSqr() > 5.0 * 5.0) {
                            Vec3 horizontal = new Vec3(move.x, 0, move.z).normalize();
                            Vec3 attempt = encounter.location.add(horizontal.scale(.17));
                            if (level.getBlockState(BlockPos.containing(attempt)).isAir())
                                nextLocation = attempt;
                        } else {
                            observedTicks = sightThreshold;
                        }
                    } else if (encounter.kind == EncounterKind.FOREST && !observed
                               && elapsedTicks > 0 && elapsedTicks % 100 == 0) {
                        // Briefly steps sideways, as though weaving between distant trees.
                        Vec3 side = player.getLookAngle();
                        Vec3 horizontal = new Vec3(-side.z, 0, side.x).normalize();
                        Vec3 attempt = encounter.location.add(horizontal.scale(level.getRandom().nextBoolean() ? 3 : -3));
                        if (level.hasChunkAt(BlockPos.containing(attempt))
                            && level.getBlockState(BlockPos.containing(attempt)).isAir()) {
                            nextLocation = attempt;
                        }
                    } else if (encounter.kind == EncounterKind.SLEEP) {
                        // Levitation and soft side-to-side movement; no grounded footsteps.
                        double bob = Math.sin(elapsedTicks * .11) * .055;
                        nextLocation = encounter.location.add(Math.sin(elapsedTicks * .07) * .04, bob, 0);
                    }
                    if (observedTicks >= sightThreshold) {
                        level.sendParticles(player, encounter.kind == EncounterKind.SLEEP
                                ? ParticleTypes.SOUL : ParticleTypes.POOF, true, false,
                            nextLocation.x, nextLocation.y, nextLocation.z,
                            8, .35, .8, .35, .02);
                        sendSound(player, level, nextLocation);
                        ENCOUNTERS.remove(id);
                        clearBody(id);
                        continue;
                    }
                    moveBody(player, nextLocation, encounter.kind, elapsedTicks);
                    if (tick % 10 == 0) render(player, level,
                        new Encounter(encounter.kind, encounter.dimension,
                                      nextLocation, encounter.remainingTicks, observedTicks));
                    ENCOUNTERS.put(id, new Encounter(encounter.kind, encounter.dimension,
                            nextLocation, encounter.remainingTicks - 10, observedTicks));
                    continue;
                }
                int cooldown = COOLDOWNS.getOrDefault(id, 0);
                if (cooldown > 0) { COOLDOWNS.put(id, Math.max(0,cooldown-10)); continue; }
                if (level.getRandom().nextInt(600) != 0) continue;
                EncounterKind chosen;
                if (player.getBlockY() < 55 && level.getMaxLocalRawBrightness(player.blockPosition()) <= 7) chosen=EncounterKind.CAVE;
                else if (level.getMaxLocalRawBrightness(player.blockPosition().above()) <= 7) chosen=EncounterKind.FOREST;
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
        clearBody(player.getUUID());
        spawnBody(player, level, location, kind);
        ENCOUNTERS.put(player.getUUID(),new Encounter(kind,level.dimension().identifier().toString(),location,500,0));
        COOLDOWNS.put(player.getUUID(),2400);
        player.sendSystemMessage(Component.literal("[DNHT v6] " + kind.name().toLowerCase() + " stalker spawned to your left. Each type now behaves differently."));
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
    // Each figure has ten independently positioned three-dimensional parts.
    // The detailed sprite remains the front-facing appearance; these add depth from side angles.
    private static void spawnBody(ServerPlayer player, ServerLevel level, Vec3 origin, EncounterKind kind) {
        BlockState surface = switch (kind) {
            case CAVE -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            case FOREST -> Blocks.DARK_OAK_LOG.defaultBlockState();
            case WINDOW -> Blocks.CALCITE.defaultBlockState();
            case SLEEP -> Blocks.SCULK.defaultBlockState();
        };
        BlockState secondary = switch (kind) {
            case CAVE -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case FOREST -> Blocks.MANGROVE_ROOTS.defaultBlockState();
            case WINDOW -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case SLEEP -> Blocks.AMETHYST_BLOCK.defaultBlockState();
        };
        // Offsets are relative to the 2D figure's midpoint; depth offsets make a solid silhouette.
        float[][] parts = {
            // dx, dy, dz, width, height, depth
            {-0.33f,-0.54f,0.04f,0.65f,1.12f,0.44f}, // chest / torso
            {-0.22f, 0.45f,0.02f,0.44f,0.46f,0.41f}, // head
            {-0.61f,-0.50f,0.01f,0.22f,0.91f,0.27f}, // left arm
            { 0.40f,-0.50f,0.01f,0.22f,0.91f,0.27f}, // right arm
            {-0.26f,-1.37f,0.01f,0.22f,0.91f,0.27f}, // left leg
            { 0.05f,-1.37f,0.01f,0.22f,0.91f,0.27f}, // right leg
            {-0.42f, 0.13f,0.02f,0.19f,0.28f,0.24f}, // shoulders
            { 0.24f, 0.13f,0.02f,0.19f,0.28f,0.24f},
            {-0.25f,-0.20f,-0.30f,0.48f,0.72f,0.27f}, // back layer
            {-0.18f,-0.80f,0.34f,0.36f,0.56f,0.14f}  // front relief
        };
        List<Display.BlockDisplay> displays = new ArrayList<>();
        for (int i=0;i<parts.length;i++) {
            Display.BlockDisplay part = ((EntityType<Display.BlockDisplay>) BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("minecraft", "block_display"))).create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (part == null) continue;
            float[] p = parts[i];
            part.setBlockState(i >= 6 ? secondary : surface);
            part.setPos(origin.x + p[0], origin.y + p[1], origin.z + p[2]);
            part.setTransformation(new Transformation(
                new Vector3f(0f,0f,0f), new Quaternionf(),
                new Vector3f(p[3],p[4],p[5]), new Quaternionf()));
            level.addFreshEntity(part);
            displays.add(part);
        }
        BODY_PARTS.put(player.getUUID(), displays);
    }

    private static void moveBody(ServerPlayer player, Vec3 origin, EncounterKind kind, int elapsed) {
        List<Display.BlockDisplay> parts = BODY_PARTS.get(player.getUUID());
        if (parts == null || parts.size() != 10) return;
        float[][] offsets = {
            {-0.33f,-0.54f,0.04f},{-0.22f,0.45f,0.02f},
            {-0.61f,-0.50f,0.01f},{0.40f,-0.50f,0.01f},
            {-0.26f,-1.37f,0.01f},{0.05f,-1.37f,0.01f},
            {-0.42f,0.13f,0.02f},{0.24f,0.13f,0.02f},
            {-0.25f,-0.20f,-0.30f},{-0.18f,-0.80f,0.34f}
        };
        double breathing = Math.sin(elapsed * .11) * .04;
        double armSway = Math.sin(elapsed * .15) * .13;
        for (int i=0;i<parts.size();i++) {
            float[] offset = offsets[i];
            double x=origin.x + offset[0];
            double y=origin.y + offset[1] + (kind == EncounterKind.SLEEP ? breathing : 0);
            if (i==0 || i==1) y += breathing;
            if (i==2) x -= armSway;
            if (i==3) x += armSway;
            parts.get(i).setPos(x,y,origin.z+offset[2]);
        }
    }

    private static void clearBody(UUID id) {
        List<Display.BlockDisplay> parts = BODY_PARTS.remove(id);
        if (parts != null) for (Display.BlockDisplay part : parts) part.discard();
    }

    private static void render(ServerPlayer player, ServerLevel level, Encounter encounter) {
        Vec3 p = encounter.location;
        // Dedicated texture-based player-facing figure, not vanilla smoke.
        SimpleParticleType sprite = switch (encounter.kind) {
            case CAVE -> CAVE_SPRITE;
            case FOREST -> FOREST_SPRITE;
            case WINDOW -> WINDOW_SPRITE;
            case SLEEP -> SLEEP_SPRITE;
        };
        level.sendParticles(player, sprite, true, false,
                p.x, p.y, p.z, 1, 0, 0, 0, 0);
        // Keep the original prototype's subtle atmospheric secondary effects.
        if (level.getRandom().nextInt(4) == 0) {
            switch (encounter.kind) {
                case CAVE -> level.sendParticles(player, ParticleTypes.ASH, true, false,
                        p.x, p.y - 0.7, p.z, 2, .3, .2, .3, .001);
                case WINDOW -> level.sendParticles(player, ParticleTypes.END_ROD, true, false,
                        p.x, p.y + .55, p.z, 1, .05, .03, .05, 0);
                case FOREST -> level.sendParticles(player, ParticleTypes.ASH, true, false,
                        p.x, p.y, p.z, 2, .5, .7, .5, .001);
                case SLEEP -> level.sendParticles(player, ParticleTypes.SOUL, true, false,
                        p.x, p.y - .3, p.z, 1, .2, .2, .2, .001);
            }
        }
    }
    private static void sendSound(ServerPlayer player,ServerLevel level,Vec3 location) {
        player.connection.send(new ClientboundSoundPacket(
            BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ENDERMAN_STARE),
            SoundSource.AMBIENT,location.x,location.y,location.z,
            0.75f,0.65f+level.getRandom().nextFloat()*.3f,level.getRandom().nextLong()));
    }
    private static int clear(ServerPlayer player) {
        ENCOUNTERS.remove(player.getUUID());
        clearBody(player.getUUID());
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
