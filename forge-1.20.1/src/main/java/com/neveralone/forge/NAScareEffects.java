package com.neveralone.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Stage 2 test effects. Entirely server-side, temporary, no world block changes,
 * no real stalker entity and no claim of having ported the Fabric scare director.
 */
public final class NAScareEffects {
    private NAScareEffects() { }

    public static int trigger(ServerPlayer player, int id) {
        ServerLevel level = player.serverLevel();
        Vec3 behind = player.position().subtract(player.getLookAngle().scale(3.0D));
        BlockPos soundPos = BlockPos.containing(behind.x, behind.y, behind.z);
        double x = behind.x;
        double y = behind.y + 1.0D;
        double z = behind.z;

        switch (id) {
            case 1: // Something moves behind you.
                level.playSound(null, soundPos, SoundEvents.GRAVEL_STEP, SoundSource.AMBIENT, 0.9F, 0.64F);
                level.sendParticles(ParticleTypes.SMOKE, x, y - 0.8D, z, 7, 0.35D, 0.07D, 0.35D, 0.015D);
                break;
            case 2: // Door somewhere nearby.
                level.playSound(null, soundPos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.AMBIENT, 0.95F, 0.66F);
                level.playSound(null, soundPos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.AMBIENT, 0.45F, 0.52F);
                break;
            case 3: // Torch sputter (visual only; blocks stay untouched).
                level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT, 0.75F, 1.4F);
                level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.3D, player.getZ(),
                    17, 0.5D, 0.5D, 0.5D, 0.01D);
                break;
            case 4: // Brief eerie darkness, with short-lived soul particles.
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, false, false, true));
                level.sendParticles(ParticleTypes.SOUL, x, y, z, 14, 0.6D, 0.7D, 0.6D, 0.025D);
                break;
            case 5: // Distant presence.
                level.playSound(null, soundPos, SoundEvents.ENDERMAN_STARE, SoundSource.AMBIENT, 0.55F, 0.55F);
                level.sendParticles(ParticleTypes.PORTAL, x, y, z, 16, 0.45D, 0.7D, 0.45D, 0.03D);
                break;
            case 6: // Flicker behind your shoulders.
                level.sendParticles(ParticleTypes.SMOKE, x, y, z, 26, 0.8D, 0.8D, 0.8D, 0.018D);
                level.playSound(null, soundPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT, 0.5F, 0.48F);
                break;
            case 7: // A brief sound, then silence.
                level.playSound(null, soundPos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.AMBIENT, 0.45F, 0.54F);
                level.sendParticles(ParticleTypes.PORTAL, x, y, z, 23, 0.7D, 0.8D, 0.7D, 0.06D);
                break;
            case 8: // Stronger combined test, no combat or world edits.
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 75, 0, false, false, true));
                level.playSound(null, soundPos, SoundEvents.ENDERMAN_STARE, SoundSource.AMBIENT, 0.85F, 0.64F);
                level.sendParticles(ParticleTypes.SOUL, x, y, z, 27, 0.85D, 0.9D, 0.85D, 0.05D);
                break;
            default:
                return 0;
        }
        return 1;
    }
}
