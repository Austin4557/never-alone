package net.locallupo.whisperingspirits;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.RandomSource;
import net.minecraft.core.particles.SimpleParticleType;

public final class WatcherEyesParticle extends SingleQuadParticle {
    protected WatcherEyesParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites.get(0, 1));
        this.gravity = 0;
        this.hasPhysics = false;
        this.lifetime = 12;
        this.quadSize = 0.35f;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.setAlpha(1f);
    }
    @Override public void tick() {
        super.tick();
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
    }
    @Override protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }
    @Override public int getLightCoords(float tint) {
        return 0xF000F0;
    }
    public static final class Factory implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Factory(SpriteSet sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double vx, double vy, double vz, RandomSource random) {
            return new WatcherEyesParticle(level, x, y, z, sprites);
        }
    }
}
