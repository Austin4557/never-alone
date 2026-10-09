package net.locallupo.whisperingspirits;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;

public final class WhisperingSpiritsClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ParticleProviderRegistry.getInstance().register(WhisperingSpiritsFabric.WATCHER_EYES_PARTICLE, WatcherEyesParticle.Factory::new);
    }
}
