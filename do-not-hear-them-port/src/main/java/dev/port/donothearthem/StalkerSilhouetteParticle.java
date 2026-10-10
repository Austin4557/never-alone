package dev.port.donothearthem;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public final class StalkerSilhouetteParticle extends SingleQuadParticle {
    private final SpriteSet frames;
    private final double originX;
    private final double originY;
    private final double originZ;
    private final float baseSize;
    private final int animationOffset;

    protected StalkerSilhouetteParticle(ClientLevel level, double x, double y, double z,
                                        SpriteSet frames, RandomSource random) {
        super(level, x, y, z, frames.get(0, 3));
        this.frames = frames;
        this.originX = x;
        this.originY = y;
        this.originZ = z;
        this.animationOffset = random.nextInt(4);
        this.gravity = 0;
        this.hasPhysics = false;
        this.lifetime = 12;
        this.baseSize = 2.25f + random.nextFloat() * .12f;
        this.quadSize = this.baseSize;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.setAlpha(.92f);
    }

    @Override public void tick() {
        super.tick();
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        // Four-frame blink sequence, subtle breathing and independent spectral sway.
        int frame = ((this.age / 3) + animationOffset) % 4;
        this.setSprite(frames.get(frame, 3));
        double pulse = Math.sin((this.age + animationOffset * 3) * .45);
        this.quadSize = baseSize * (1f + (float) pulse * .025f);
        this.setPos(originX + Math.sin((this.age + animationOffset) * .22) * .015,
                    originY + Math.sin((this.age + animationOffset) * .28) * .028, originZ);
        float fadeIn = Math.min(1f, (this.age + 1) / 2.5f);
        float fadeOut = Math.min(1f, (lifetime - this.age) / 3.0f);
        this.setAlpha(Math.max(0f, Math.min(1f, fadeIn * fadeOut)) * .94f);
    }

    @Override protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override public int getLightCoords(float partialTick) {
        return 0xF000F0;
    }

    public static final class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Factory(SpriteSet sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
            return new StalkerSilhouetteParticle(level, x, y, z, sprites, random);
        }
    }
}
