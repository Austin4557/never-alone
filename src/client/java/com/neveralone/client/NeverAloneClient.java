package com.neveralone.client;

import java.util.concurrent.ThreadLocalRandom;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import com.neveralone.NeverAlone;

public final class NeverAloneClient implements ClientModInitializer {
    private static final int MIN_DELAY_TICKS = 12 * 60 * 20;
    private static final int MAX_DELAY_TICKS = 45 * 60 * 20;

    private static final Identifier[] IMAGES = {
        NeverAlone.id("textures/gui/scare_1.png"),
        NeverAlone.id("textures/gui/scare_2.png"),
        NeverAlone.id("textures/gui/scare_3.png"),
        NeverAlone.id("textures/gui/scare_4.png")
    };

    private static final SoundEvent[] SOUNDS = {
        NeverAlone.SCARE_1, NeverAlone.SCARE_2, NeverAlone.SCARE_3, NeverAlone.SCARE_4
    };

    private int ticksUntilScare;
    private int scareTicks;
    private int scareIndex;
    private boolean wasInWorld;

    @Override
    public void onInitializeClient() {
        resetTimer();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudElementRegistry.addLast(NeverAlone.id("jumpscare"), (graphics, deltaTracker) -> {
            if (scareTicks <= 0) return;
            Minecraft client = Minecraft.getInstance();
            int width = client.getWindow().getGuiScaledWidth();
            int height = client.getWindow().getGuiScaledHeight();
            graphics.fill(0, 0, width, height, 0xFF000000);
            graphics.blit(RenderPipelines.GUI_TEXTURED, IMAGES[scareIndex],
                0, 0, 0.0F, 0.0F, width, height, width, height);
        });
    }

    private void tick(Minecraft client) {
        boolean playable = client.player != null && client.level != null
            && client.getScreen() == null && !client.isPaused();
        if (!playable) { wasInWorld = false; return; }
        if (!wasInWorld) { wasInWorld = true; return; }
        if (scareTicks > 0) { scareTicks--; return; }
        if (--ticksUntilScare <= 0) triggerScare(client);
    }

    private void triggerScare(Minecraft client) {
        int previous = scareIndex;
        do {
            scareIndex = ThreadLocalRandom.current().nextInt(IMAGES.length);
        } while (IMAGES.length > 1 && scareIndex == previous);
        scareTicks = ThreadLocalRandom.current().nextInt(11, 19);
        float pitch = ThreadLocalRandom.current().nextFloat(0.92F, 1.09F);
        client.getSoundManager().play(SimpleSoundInstance.forUI(SOUNDS[scareIndex], pitch, 1.0F));
        resetTimer();
    }

    private void resetTimer() {
        ticksUntilScare = ThreadLocalRandom.current().nextInt(MIN_DELAY_TICKS, MAX_DELAY_TICKS + 1);
    }
}
