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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
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
    // Pure 3D segmented bodies; the illustrated sprite is a fallback if 3D spawning fails.
    private static final Map<UUID, List<BodyPart>> BODY_PARTS = new HashMap<>();
    private static final BlockState[][] CREATURE_SURFACES = new BlockState[4][4];
    private MinecraftServer previousServer;
    private int tick;

    @Override public void onInitialize() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("donothearthemprototype", "stalker_cave"), CAVE_SPRITE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("donothearthemprototype", "stalker_forest"), FOREST_SPRITE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("donothearthemprototype", "stalker_window"), WINDOW_SPRITE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath("donothearthemprototype", "stalker_sleep"), SLEEP_SPRITE);
        registerCreatureSurfaces();
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
                    int elapsedTicks = Math.max(0, (encounter.kind == EncounterKind.FOREST ? 1200 : 500) - encounter.remainingTicks);
                    Vec3 nextLocation = encounter.location;
                    int sightThreshold = switch (encounter.kind) {
                        case CAVE -> 50;     // Keeps advancing until faced down.
                        case FOREST -> elapsedTicks < 300 ? 10000 : 120; // Give the player at least 15 seconds to inspect the forest model.
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
        ENCOUNTERS.put(player.getUUID(),new Encounter(kind,level.dimension().identifier().toString(),location,kind==EncounterKind.FOREST?1200:500,0));
        COOLDOWNS.put(player.getUUID(),2400);
        player.sendSystemMessage(Component.literal("[DNHT v17] " + kind.name().toLowerCase() + " stalker spawned to your left. Each type now behaves differently."));
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

    // Independent 3D parts (true rotated cuboids) with four distinct anatomies.
    // Kept behind the high-detail v7/v8 illustrated sprite, which remains the face layer.
    private record Shape(float x,float y,float z,float w,float h,float d,float radians,int material,int motion) {}
    private record BodyPart(Display.BlockDisplay entity, Shape shape) {}
    private static final float R = (float) Math.PI / 180f;

    private static void piece(List<Shape> shapes, float x,float y,float z,
                              float w,float h,float depth,float angle,int material,int motion) {
        shapes.add(new Shape(x,y,z,w,h,depth,angle*R,material,motion));
    }
    private static void bone(List<Shape> shapes, float x1,float y1,float x2,float y2,
                             float depth,float width,float z,int material,int motion) {
        float dx=x2-x1, dy=y2-y1;
        float length=(float)Math.hypot(dx,dy);
        float angle=(float)Math.atan2(-dx,dy);
        shapes.add(new Shape((x1+x2)/2f,(y1+y2)/2f,z,width,length,depth,angle,material,motion));
    }
    private static List<Shape> geometry(EncounterKind kind) {
        // v17: four radically different silhouettes inspired by the original
        // Forge mod's entity illustrations. All parts are truly three-dimensional.
        // We do not redistribute the original mod's copyrighted textures.
        List<Shape> s = new ArrayList<>();
        switch (kind) {
            case CAVE -> {
                // SUN-EYE: a towering tapering shadow, a single large cyclopean
                // eye, and an uneven crown of radiating horn-like spines.
                piece(s,0,-.55f,-.27f,.68f,1.65f,.56f,0,0,0);
                piece(s,0,.25f,-.31f,.92f,.78f,.66f,0,0,0);
                piece(s,0,.90f,-.28f,.66f,.74f,.61f,0,0,1);
                piece(s,0,1.02f,.065f,.43f,.42f,.17f,0,1,1);
                piece(s,0,1.02f,.173f,.225f,.23f,.095f,0,3,1);
                piece(s,0,1.02f,.240f,.11f,.12f,.07f,0,0,1);
                piece(s,0,-1.46f,-.28f,.76f,.40f,.57f,0,1,0);
                for(int side : new int[]{-1,1}) {
                    // Uneven arm spines, but no hunched crawler silhouette.
                    bone(s,side*.33f,.33f,side*.58f,-.37f,.23f,.22f,-.34f,0,side<0?2:3);
                    bone(s,side*.58f,-.37f,side*.77f,-1.11f,.16f,.15f,-.35f,0,side<0?2:3);
                    for(int finger=0;finger<3;finger++)
                        bone(s,side*.77f,-1.12f,side*(.84f+finger*.10f),
                            -1.46f-finger*.09f,.10f,.066f,-.24f,1,side<0?2:3);
                    // Starburst crown made from narrow branching beams.
                    bone(s,side*.21f,1.18f,side*.77f,1.52f,.17f,.13f,-.31f,2,6);
                    bone(s,side*.24f,1.31f,side*.43f,2.02f,.14f,.105f,-.31f,2,6);
                    bone(s,side*.18f,.84f,side*.83f,.88f,.12f,.105f,-.32f,2,6);
                    bone(s,side*.33f,.80f,side*.62f,.44f,.14f,.10f,-.31f,2,6);
                }
                bone(s,0,1.37f,0,2.16f,.18f,.16f,-.31f,2,6);
                for(int i=0;i<5;i++) {
                    float x=(i-2)*.145f;
                    piece(s,x,-.55f-i%2*.19f,.028f,.09f,.82f,.13f,(i-2)*5,1,0);
                }
            }
            case FOREST -> {
                // CROOKED WATCHER: long bent neck, narrow rib cage, crooked
                // arms and tall twin horns. No bulky timber chest or broad antlers.
                piece(s,0,-.39f,-.27f,.49f,1.38f,.37f,-4,0,0);
                piece(s,0,.39f,-.29f,.55f,.43f,.41f,3,1,0);
                piece(s,0,-1.10f,-.28f,.38f,.45f,.35f,0,0,0);
                bone(s,0,.48f,-.15f,1.04f,.29f,.25f,-.30f,0,1);
                piece(s,-.19f,1.26f,-.29f,.45f,.58f,.39f,-12,0,1);
                piece(s,-.20f,1.20f,-.052f,.31f,.42f,.11f,-12,3,1);
                for(int side :new int[]{-1,1}) {
                    // Twin crooked horns, asymmetric branch tips.
                    float sx=side*.17f-.20f;
                    bone(s,sx,1.48f,sx+side*.28f,1.93f,.15f,.12f,-.31f,1,6);
                    bone(s,sx+side*.28f,1.93f,sx+side*.21f,2.25f,.12f,.09f,-.31f,0,6);
                    if(side<0) bone(s,sx+side*.25f,1.88f,sx+side*.53f,2.05f,.10f,.075f,-.31f,1,6);
                    // Shoulders connect with unusually long crooked arms.
                    bone(s,side*.14f,.44f,side*.42f,.26f,.25f,.23f,-.30f,0,0);
                    bone(s,side*.42f,.26f,side*.88f,-.45f,.25f,.18f,-.30f,0,side<0?2:3);
                    bone(s,side*.88f,-.45f,side*.81f,-1.39f,.20f,.14f,-.29f,0,side<0?2:3);
                    for(int i=0;i<4;i++)
                        bone(s,side*.81f,-1.36f,side*(.85f+i*.105f),-1.70f-i*.085f,
                            .09f,.065f,-.22f,1,side<0?2:3);
                    // Stilt-like legs unlike the former bulky forest mannequin.
                    bone(s,side*.14f,-1.06f,side*.30f,-1.91f,.20f,.17f,-.31f,0,4+(side>0?1:0));
                    bone(s,side*.30f,-1.91f,side*.46f,-2.45f,.16f,.12f,-.31f,0,4+(side>0?1:0));
                    bone(s,side*.46f,-2.44f,side*.69f,-2.57f,.16f,.09f,-.19f,1,0);
                }
                // Exposed rib arches and extremely narrow hanging root fibers.
                for(int i=0;i<4;i++) {
                    float y=.16f-i*.19f;
                    bone(s,-.29f,y,-.04f,y-.11f,.10f,.075f,.005f,1,0);
                    bone(s,.29f,y,.04f,y-.11f,.10f,.075f,.005f,1,0);
                }
                for(int i=0;i<5;i++)
                    piece(s,(i-2)*.09f,-.45f-i%2*.16f,.005f,.05f,.60f,.10f,i*3,2,0);
            }
            case WINDOW -> {
                // PALE WINDOW FACE: oversized uncanny mask, almost featureless
                // black draping body and tiny reaching fingers. Unlike forest.
                piece(s,0,-.50f,-.28f,.78f,1.75f,.48f,0,0,0);
                piece(s,0,.27f,-.27f,.99f,.52f,.56f,0,0,0);
                piece(s,0,.85f,-.31f,.84f,.91f,.66f,0,3,1);
                // Black empty eye sockets and a dark oval mouth.
                piece(s,-.21f,.97f,.045f,.22f,.28f,.095f,0,0,1);
                piece(s,.21f,.97f,.045f,.22f,.28f,.095f,0,0,1);
                piece(s,0,.51f,.06f,.27f,.23f,.105f,0,0,1);
                for(int side:new int[]{-1,1}) {
                    piece(s,side*.48f,.15f,-.29f,.28f,.44f,.44f,side*13,0,0);
                    bone(s,side*.46f,.20f,side*.73f,-.65f,.29f,.21f,-.31f,0,side<0?2:3);
                    bone(s,side*.73f,-.65f,side*.93f,-1.55f,.20f,.14f,-.31f,0,side<0?2:3);
                    for(int i=0;i<4;i++)
                        bone(s,side*.93f,-1.54f,side*(1.03f+i*.11f),-1.88f-i*.06f,
                            .09f,.065f,-.25f,1,side<0?2:3);
                    // No ordinary boots: the dark coat tapers into ragged points.
                    for(int i=0;i<3;i++)
                        bone(s,side*(.18f+i*.12f),-1.20f,
                            side*(.24f+i*.15f),-1.95f+(i%2)*.17f,.15f,.10f,-.32f,0,7);
                }
                piece(s,0,-.97f,.02f,.52f,.76f,.20f,0,1,0);
                for(int i=0;i<5;i++)
                    bone(s,(i-2)*.14f,-1.15f,(i-2)*.20f,-2.00f+(i%2)*.13f,
                        .12f,.095f,-.13f,0,7);
            }
            case SLEEP -> {
                // SLEEP FIGURE: oversized smooth pale head, pencil-thin humanoid
                // torso and long hanging arms, replacing the bulky robe/wraith skirt.
                piece(s,0,.91f,-.29f,.77f,.93f,.56f,0,3,1);
                piece(s,0,.42f,-.30f,.26f,.31f,.27f,0,1,1);
                piece(s,0,-.41f,-.30f,.45f,1.37f,.35f,0,0,0);
                piece(s,0,-1.07f,-.30f,.47f,.28f,.34f,0,0,0);
                for(int side:new int[]{-1,1}) {
                    // Hollow black sockets with tiny lavender pinpoint pupils.
                    piece(s,side*.21f,.98f,.015f,.22f,.27f,.095f,0,0,1);
                    piece(s,side*.21f,1.00f,.084f,.065f,.075f,.052f,0,1,1);
                    bone(s,side*.20f,.24f,side*.39f,-.69f,.19f,.16f,-.30f,0,side<0?2:3);
                    bone(s,side*.39f,-.69f,side*.53f,-1.65f,.14f,.12f,-.30f,0,side<0?2:3);
                    for(int i=0;i<4;i++)
                        bone(s,side*.53f,-1.63f,side*(.55f+i*.085f),-1.94f-i*.06f,
                            .08f,.052f,-.21f,1,side<0?2:3);
                    bone(s,side*.13f,-1.16f,side*.22f,-1.89f,.20f,.14f,-.30f,0,4+(side>0?1:0));
                    bone(s,side*.22f,-1.87f,side*.35f,-2.23f,.16f,.11f,-.30f,0,4+(side>0?1:0));
                }
                // Slim ribs, not a layered skirt.
                for(int i=0;i<4;i++) {
                    float y=.05f-i*.25f;
                    bone(s,-.18f,y,-.045f,y-.13f,.085f,.06f,-.065f,1,0);
                    bone(s,.18f,y,.045f,y-.13f,.085f,.06f,-.065f,1,0);
                }
            }
        }
        return s;
    }
    // Unique SOLID 3D materials: opaque and colliding block properties matter here
    // even though no blocks are placed: the shaders should not classify these as
    // non-occluding/translucent. Display entities themselves never have collision.
    // No vanilla world textures or block replacements.
    // These technical blocks have no item, are never placed in the world, and are
    // rendered exclusively through BlockDisplay entities in the encounters.
    private static void registerCreatureSurfaces() {
        String[] kinds = {"cave", "forest", "window", "sleep"};
        for (int k = 0; k < kinds.length; k++) {
            for (int material = 0; material < 4; material++) {
                Identifier id = Identifier.fromNamespaceAndPath(
                    "donothearthemprototype", "skin_" + kinds[k] + "_" + material);
                ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
                Block block = new Block(BlockBehaviour.Properties.of()
                    .setId(key).strength(-1f));
                Registry.register(BuiltInRegistries.BLOCK, key, block);
                CREATURE_SURFACES[k][material] = block.defaultBlockState();
            }
        }
    }
    private static BlockState blockFor(EncounterKind kind, int material) {
        int kindIndex = switch (kind) {
            case CAVE -> 0;
            case FOREST -> 1;
            case WINDOW -> 2;
            case SLEEP -> 3;
        };
        return CREATURE_SURFACES[kindIndex][Math.max(0,Math.min(3,material))];
    }
    private static void spawnBody(ServerPlayer player, ServerLevel level, Vec3 origin, EncounterKind kind) {
        EntityType<?> registeredType = BuiltInRegistries.ENTITY_TYPE.getValue(
            Identifier.fromNamespaceAndPath("minecraft", "block_display"));
        if (registeredType == null) {
            player.sendSystemMessage(Component.literal("[DNHT v17] 3D display unavailable; sprite mode active."));
            return;
        }
        List<BodyPart> displays = new ArrayList<>();
        for (Shape shape : geometry(kind)) {
            Display.BlockDisplay display = (Display.BlockDisplay) registeredType.create(
                level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (display == null) continue;
            display.setBlockState(blockFor(kind,shape.material));
            display.setPos(origin.x+shape.x,origin.y+shape.y,origin.z+shape.z);
            display.setTransformation(new Transformation(
                new Vector3f(-shape.w/2,-shape.h/2,-shape.d/2).rotateZ(shape.radians),
                new Quaternionf().rotationZ(shape.radians),
                new Vector3f(shape.w,shape.h,shape.d),
                new Quaternionf()));
            level.addFreshEntity(display);
            displays.add(new BodyPart(display,shape));
        }
        BODY_PARTS.put(player.getUUID(),displays);
    }
    private static void moveBody(ServerPlayer player, Vec3 origin, EncounterKind kind, int elapsed) {
        List<BodyPart> parts=BODY_PARTS.get(player.getUUID());
        if(parts==null)return;
        float breath=(float)Math.sin(elapsed*.11)*.035f;
        float sway=(float)Math.sin(elapsed*.09)*.085f;
        for (BodyPart body : parts) {
            Shape s=body.shape();
            int motion=s.motion();
            double x=origin.x+s.x+(motion==2?-sway:motion==3?sway:0);
            double y=origin.y+s.y+((motion==0||motion==1)?breath:0)
                +(kind==EncounterKind.SLEEP?Math.sin(elapsed*.065)*.10:0);
            double z=origin.z+s.z+((motion==7)?Math.sin(elapsed*.09+s.x*4)*.10:0);
            body.entity().setPos(x,y,z);
        }
    }
    private static void clearBody(UUID id) {
        List<BodyPart> parts=BODY_PARTS.remove(id);
        if(parts!=null)for(BodyPart part:parts)part.entity().discard();
    }

    private static void render(ServerPlayer player, ServerLevel level, Encounter encounter) {
        Vec3 p = encounter.location;
        // A successfully spawned 3D body must be the only full-body renderer.
        // Preserve the old sprite only as a fallback when no 3D parts exist.
        List<BodyPart> body = BODY_PARTS.get(player.getUUID());
        if (body == null || body.isEmpty()) {
            SimpleParticleType sprite = switch (encounter.kind) {
                case CAVE -> CAVE_SPRITE;
                case FOREST -> FOREST_SPRITE;
                case WINDOW -> WINDOW_SPRITE;
                case SLEEP -> SLEEP_SPRITE;
            };
            level.sendParticles(player, sprite, true, false,
                    p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
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
