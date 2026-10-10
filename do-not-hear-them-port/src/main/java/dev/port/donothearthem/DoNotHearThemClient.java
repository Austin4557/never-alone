package dev.port.donothearthem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;

public final class DoNotHearThemClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ParticleProviderRegistry.getInstance().register(DoNotHearThemPrototype.CAVE_SPRITE, StalkerSilhouetteParticle.Factory::new);
        ParticleProviderRegistry.getInstance().register(DoNotHearThemPrototype.FOREST_SPRITE, StalkerSilhouetteParticle.Factory::new);
        ParticleProviderRegistry.getInstance().register(DoNotHearThemPrototype.WINDOW_SPRITE, StalkerSilhouetteParticle.Factory::new);
        ParticleProviderRegistry.getInstance().register(DoNotHearThemPrototype.SLEEP_SPRITE, StalkerSilhouetteParticle.Factory::new);
    }
}
