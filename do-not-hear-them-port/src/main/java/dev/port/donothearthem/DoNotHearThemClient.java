package dev.port.donothearthem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;

public final class DoNotHearThemClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ParticleProviderRegistry.getInstance().register(DoNotHearThemPrototype.STALKER_SPRITE, StalkerSilhouetteParticle.Factory::new);
    }
}
