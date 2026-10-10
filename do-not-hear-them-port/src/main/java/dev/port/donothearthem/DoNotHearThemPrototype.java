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
    // Pure 3D segmented bodies; the illustrated sprite is a fallback if 3D spawning fails.
    private static final Map<UUID, List<BodyPart>> BODY_PARTS = new HashMap<>();
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
        player.sendSystemMessage(Component.literal("[DNHT v13] " + kind.name().toLowerCase() + " stalker spawned to your left. Each type now behaves differently."));
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
        List<Shape> s = new ArrayList<>();
        switch (kind) {
            case CAVE -> {
                // A broad, squat crawler: articulated back, rock ridges, forward hanging arms.
                piece(s,0,-.08f,-.25f,1.28f,.69f,.72f,-12,0,0);
                piece(s,0,-.49f,-.18f,1.05f,.66f,.6f,5,0,0);
                piece(s,0,.43f,-.30f,.64f,.57f,.58f,15,1,1);
                piece(s,0,.42f,.08f,.41f,.31f,.14f,12,2,1);
                for(int i=0;i<5;i++) piece(s,-.52f+i*.26f,.38f,-.58f,.20f,.48f,.32f,(i-2)*10,1,0);
                bone(s,-.46f,.12f,-1.03f,-.49f,.33f,.30f,-.34f,0,2);
                bone(s,-1.03f,-.49f,-1.34f,-1.37f,.28f,.23f,-.23f,1,2);
                bone(s,.46f,.12f,1.03f,-.49f,.33f,.30f,-.34f,0,3);
                bone(s,1.03f,-.49f,1.34f,-1.37f,.28f,.23f,-.23f,1,3);
                bone(s,-.32f,-.70f,-.63f,-1.18f,.41f,.37f,-.23f,0,4);
                bone(s,-.63f,-1.18f,-.41f,-1.65f,.38f,.3f,-.12f,1,4);
                bone(s,.32f,-.70f,.63f,-1.18f,.41f,.37f,-.23f,0,5);
                bone(s,.63f,-1.18f,.41f,-1.65f,.38f,.3f,-.12f,1,5);
                for(int side : new int[]{-1,1}) {
                    for(int i=0;i<3;i++) bone(s,side*1.34f,-1.34f,side*(1.54f+i*.12f),-1.71f,.12f,.085f,.02f,2,side<0?2:3);
                    for(int i=0;i<3;i++) bone(s,side*.45f,-1.62f,side*(.71f+i*.1f),-1.82f,.15f,.10f,.01f,2,0);
                }
                for(int i=0;i<6;i++) piece(s,-.55f+i*.22f,.06f+i%2*.12f,-.63f,.15f,.36f,.19f,i%2==0?-27:26,2,0);
                // Thin ribbed relief on both flanks and inset jaw plates.
                for(int side:new int[]{-1,1}) {
                    for(int i=0;i<3;i++) bone(s,side*.29f,-.06f-i*.19f,
                        side*.56f,-.18f-i*.19f,.13f,.09f,-.64f,1,0);
                    bone(s,side*.12f,.32f,side*.22f,.06f,.15f,.09f,.04f,2,1);
                    // Broken shell shards interlock along the back and large claws.
                    bone(s,side*.49f,.28f,side*.76f,.66f,.20f,.14f,-.55f,2,0);
                    bone(s,side*.82f,.44f,side*.99f,.72f,.15f,.11f,-.57f,1,0);
                    piece(s,side*.37f,-.40f,-.67f,.18f,.36f,.09f,side*17,1,0);
                }
                for(int i=0;i<4;i++) {
                    float x=-.3f+i*.20f;
                    bone(s,x,.44f,x*.84f,-.10f,.13f,.12f,.06f,2,1);
                }
            }
            case FOREST -> {
                // Forest Hollow v12: connected organic torso, integrated shoulders and crown.
                // Every long appendage overlaps its socket so it cannot appear to float.
                piece(s,0,-.18f,-.36f,.72f,1.28f,.50f,0,0,0);       // solid central spine
                piece(s,0,.34f,-.39f,.94f,.42f,.48f,0,1,0);         // broader shoulders
                piece(s,0,-.57f,-.38f,.64f,.64f,.43f,0,0,0);        // hips
                piece(s,0,.96f,-.34f,.49f,.60f,.47f,0,0,1);        // elongated head
                piece(s,0,.84f,-.08f,.28f,.34f,.12f,0,2,1);        // mask relief
                bone(s,0,.71f,0,.40f,.39f,.35f,-.36f,1,1);          // neck socket
                for (int side : new int[]{-1,1}) {
                    // clavicles and shoulder caps bridge torso to arm.
                    bone(s,side*.14f,.45f,side*.48f,.32f,.29f,.27f,-.34f,1,0);
                    piece(s,side*.46f,.27f,-.35f,.37f,.38f,.38f,side*-13,0,0);
                    bone(s,side*.46f,.24f,side*.62f,-.53f,.28f,.28f,-.33f,0,2+(side>0?1:0));
                    bone(s,side*.62f,-.53f,side*.82f,-1.26f,.23f,.21f,-.33f,0,2+(side>0?1:0));
                    piece(s,side*.75f,-1.19f,-.34f,.23f,.23f,.25f,0,1,2+(side>0?1:0));
                    for (int i=0;i<3;i++) bone(s,side*.77f,-1.24f,
                        side*(.80f+i*.11f),-1.58f-i*.05f,.09f,.075f,-.22f,2,2+(side>0?1:0));
                    // knees/feet connect continuously to hip socket.
                    bone(s,side*.21f,-.78f,side*.29f,-1.58f,.27f,.24f,-.37f,0,4+(side>0?1:0));
                    bone(s,side*.29f,-1.56f,side*.44f,-2.20f,.23f,.22f,-.38f,0,4+(side>0?1:0));
                    piece(s,side*.43f,-2.22f,-.25f,.32f,.22f,.49f,0,1,0);
                    for(int i=0;i<3;i++) bone(s,side*.42f,-2.23f,
                        side*(.48f+i*.14f),-2.42f,.18f,.085f,-.06f,2,0);
                    // Antlers start *inside* the crown and fork naturally.
                    bone(s,side*.13f,1.15f,side*.43f,1.61f,.20f,.17f,-.37f,1,6);
                    bone(s,side*.40f,1.57f,side*.78f,1.96f,.15f,.12f,-.37f,1,6);
                    bone(s,side*.65f,1.79f,side*.97f,2.07f,.13f,.105f,-.37f,1,6);
                    bone(s,side*.37f,1.52f,side*.36f,2.01f,.13f,.10f,-.37f,1,6);
                    bone(s,side*.77f,1.95f,side*1.11f,2.06f,.11f,.075f,-.37f,1,6);
                    // Rib-like root filaments overlap and weave across the torso.
                    for(int i=0;i<3;i++) bone(s,side*.20f,.18f-i*.22f,
                        side*.04f,-.26f-i*.19f,.15f,.10f,-.65f,2,0);
                }
                // Layered tapered chest ridge and root fibers, backed by the solid trunk.
                piece(s,0,.21f,-.71f,.36f,.50f,.15f,0,1,0);
                piece(s,0,-.37f,-.72f,.27f,.69f,.12f,0,0,0);
                for(int side : new int[]{-1,1}) {
                    // Mask cheeks, brow and tiny luminous eye sockets.
                    bone(s,side*.07f,1.04f,side*.19f,.84f,.11f,.065f,-.075f,2,1);
                    bone(s,side*.04f,1.20f,side*.23f,1.17f,.12f,.11f,-.09f,1,1);
                    piece(s,side*.125f,1.065f,-.005f,.07f,.07f,.07f,0,2,1);
                    // Ragged moss fringe physically touches each clavicle.
                    for(int i=0;i<3;i++)
                        bone(s,side*(.23f+i*.10f),.29f,side*(.25f+i*.10f),-.06f-i*.06f,
                            .11f,.065f,-.68f,2,0);
                }
                // Trunk ridges and hanging moss, intentionally embedded in torso.
                for(int i=0;i<7;i++) {
                    float x=-.28f+i*.093f;
                    piece(s,x,-.17f-(i%3)*.18f,-.66f,.105f,.55f,.14f,(i-3)*3,2,0);
                }
            }
            case WINDOW -> {
                // Hollow coat, hood, dark layers and pale face made from many narrow strips.
                piece(s,0,-.48f,-.34f,.67f,1.58f,.43f,0,0,0);
                piece(s,0,.61f,-.32f,.65f,.72f,.48f,0,0,1);
                piece(s,0,.58f,.0f,.43f,.53f,.13f,0,1,1);
                piece(s,0,.24f,.10f,.22f,.12f,.15f,0,2,1);
                bone(s,-.38f,.09f,-.54f,-1.04f,.24f,.22f,-.38f,0,2);
                bone(s,.38f,.09f,.54f,-1.04f,.24f,.22f,-.38f,0,3);
                bone(s,-.19f,-1.20f,-.24f,-2.08f,.26f,.23f,-.33f,0,4);
                bone(s,.19f,-1.20f,.24f,-2.08f,.26f,.23f,-.33f,0,5);
                for(int i=0;i<7;i++) {
                    float x=-.39f+i*.13f;
                    piece(s,x,-.91f-(i%3)*.14f,-.10f,.13f,.92f,.15f,(i-3)*2,1,0);
                }
                for(int i=0;i<4;i++) {
                    bone(s,-.52f,-1.01f,-.64f+i*.09f,-1.31f,.10f,.065f,-.28f,2,2);
                    bone(s,.52f,-1.01f,.64f-i*.09f,-1.31f,.10f,.065f,-.28f,2,3);
                }
                piece(s,-.46f,.42f,-.35f,.20f,.76f,.31f,-19,0,0);
                piece(s,.46f,.42f,-.35f,.20f,.76f,.31f,19,0,0);
                piece(s,0,-.24f,.06f,.10f,1.24f,.11f,0,1,0);
                // Slim asymmetric seams and inset eye relief, instead of one smooth coat.
                for (int i=0;i<4;i++) {
                    float x=(i-1.5f)*.14f;
                    piece(s,x,-.35f-(i%2)*.15f,.14f,.065f,1.11f,.065f,(i-2)*3,1,0);
                }
                piece(s,-.13f,.68f,.11f,.095f,.075f,.055f,0,0,1);
                piece(s,.13f,.68f,.11f,.095f,.075f,.055f,0,0,1);
                // Distinct hood perimeter, raised nose, sunken face and segmented belt.
                bone(s,-.32f,.85f,-.29f,.30f,.15f,.11f,-.04f,0,1);
                bone(s,.32f,.85f,.29f,.30f,.15f,.11f,-.04f,0,1);
                bone(s,-.30f,.92f,.30f,.92f,.17f,.12f,-.05f,0,1);
                piece(s,0,.52f,.20f,.075f,.23f,.07f,0,2,1);
                piece(s,0,.19f,.15f,.18f,.055f,.05f,0,0,1);
                for(int i=0;i<5;i++)
                    piece(s,-.34f+i*.17f,-.64f,.18f,.13f,.11f,.08f,0,i%2==0?1:0,0);
                for(int side:new int[]{-1,1})
                    for(int i=0;i<3;i++)
                        bone(s,side*(.46f+i*.025f),-.25f-i*.26f,
                            side*(.48f+i*.03f),-.52f-i*.28f,.11f,.07f,-.61f,1,0);
            }
            case SLEEP -> {
                // Hollow floating wraith with drifting, hanging cloth-like segments and thin talons.
                piece(s,0,.15f,-.36f,.91f,1.10f,.52f,0,0,0);
                piece(s,0,.86f,-.32f,.69f,.77f,.49f,0,0,1);
                piece(s,0,.82f,.01f,.42f,.34f,.11f,0,1,1);
                for(int i=0;i<9;i++) {
                    float x=(i-4)*.18f;
                    piece(s,x,-.86f-(i%3)*.16f,-.3f,.16f,1.05f+(i%3)*.22f,.15f,(i-4)*4,i%3==0?2:0,7);
                }
                bone(s,-.39f,.31f,-.71f,-.61f,.20f,.24f,-.32f,0,2);
                bone(s,-.71f,-.61f,-.98f,-1.24f,.17f,.18f,-.25f,1,2);
                bone(s,.39f,.31f,.71f,-.61f,.20f,.24f,-.32f,0,3);
                bone(s,.71f,-.61f,.98f,-1.24f,.17f,.18f,-.25f,1,3);
                for(int side : new int[]{-1,1}) for(int i=0;i<4;i++)
                    bone(s,side*.98f,-1.22f,side*(1.17f+i*.10f),-1.54f,.08f,.06f,-.1f,2,side<0?2:3);
                piece(s,-.37f,.33f,-.62f,.24f,.88f,.13f,-21,2,0);
                piece(s,.37f,.33f,-.62f,.24f,.88f,.13f,21,2,0);
                for (int i=0;i<5;i++) {
                    float x=(i-2)*.20f;
                    bone(s,x,-.96f,x*.75f,-1.81f-(i%2)*.17f,.14f,.105f,-.48f,2,7);
                }
                piece(s,-.16f,.89f,.13f,.075f,.08f,.06f,0,2,1);
                piece(s,.16f,.89f,.13f,.075f,.08f,.06f,0,2,1);
                // Layered hood border, hollow center and twisting spectral ribs.
                bone(s,-.36f,1.06f,-.24f,.58f,.16f,.105f,-.07f,2,1);
                bone(s,.36f,1.06f,.24f,.58f,.16f,.105f,-.07f,2,1);
                bone(s,-.29f,1.13f,.29f,1.13f,.16f,.11f,-.07f,1,1);
                for(int side:new int[]{-1,1}) {
                    for(int i=0;i<4;i++) {
                        float y=.31f-i*.16f;
                        bone(s,side*.32f,y,side*.07f,y-.12f,.12f,.075f,-.67f,2,0);
                    }
                    bone(s,side*.26f,-.49f,side*.67f,-1.13f,.15f,.085f,-.64f,2,7);
                }
                for(int i=0;i<4;i++)
                    bone(s,(i-1.5f)*.18f,-1.20f,(i-1.5f)*.30f,-1.83f-i*.09f,.10f,.06f,-.66f,1,7);
            }
        }
        return s;
    }
    private static BlockState blockFor(EncounterKind kind,int material) {
        return switch (kind) {
            case CAVE -> switch (material) {
                case 0 -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                case 1 -> Blocks.DEEPSLATE.defaultBlockState();
                default -> Blocks.OBSIDIAN.defaultBlockState();
            };
            case FOREST -> switch (material) {
                case 0 -> Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
                case 1 -> Blocks.DARK_OAK_LOG.defaultBlockState();
                default -> Blocks.MANGROVE_ROOTS.defaultBlockState();
            };
            case WINDOW -> switch (material) {
                case 0 -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                case 1 -> Blocks.DEEPSLATE.defaultBlockState();
                default -> Blocks.CALCITE.defaultBlockState();
            };
            case SLEEP -> switch (material) {
                case 0 -> Blocks.SCULK.defaultBlockState();
                case 1 -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
                default -> Blocks.AMETHYST_BLOCK.defaultBlockState();
            };
        };
    }
    private static void spawnBody(ServerPlayer player, ServerLevel level, Vec3 origin, EncounterKind kind) {
        EntityType<?> registeredType = BuiltInRegistries.ENTITY_TYPE.getValue(
            Identifier.fromNamespaceAndPath("minecraft", "block_display"));
        if (registeredType == null) {
            player.sendSystemMessage(Component.literal("[DNHT v9] 3D display unavailable; sprite mode active."));
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
