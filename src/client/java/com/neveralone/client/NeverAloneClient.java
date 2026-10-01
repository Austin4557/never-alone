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
import com.neveralone.armor.DarkAmethystItems;
import com.neveralone.armor.RhinestoneItems;
import com.neveralone.armor.EmeraldWardenItems;
import net.rpg_foundation.armor_api.client.ArmorRenderers;
import net.rpg_foundation.armor_api.client.GeoArmorRenderer;

public final class NeverAloneClient implements ClientModInitializer {
    private static final int MIN_DELAY_TICKS = 12 * 60 * 20;
    private static final int MAX_DELAY_TICKS = 45 * 60 * 20;

    private static final Identifier[] IMAGES = {
        NeverAlone.id("textures/gui/scare_1.png"),
        NeverAlone.id("textures/gui/scare_2.png"),
        NeverAlone.id("textures/gui/scare_3.png"),
        NeverAlone.id("textures/gui/scare_4.png"),
        NeverAlone.id("textures/gui/scare_5.png"),
        NeverAlone.id("textures/gui/scare_6.png"),
        NeverAlone.id("textures/gui/scare_7.png"),
        NeverAlone.id("textures/gui/scare_8.png")
    };

    private static final SoundEvent[] SOUNDS = {
        NeverAlone.SCARE_1, NeverAlone.SCARE_2, NeverAlone.SCARE_3, NeverAlone.SCARE_4,
        NeverAlone.SCARE_5, NeverAlone.SCARE_6, NeverAlone.SCARE_7, NeverAlone.SCARE_8
    };

    private int ticksUntilScare;
    private int scareTicks;
    private int scareIndex;
    private int delayedEchoTicks;
    private int fakeOutTicks;
    private boolean wasInWorld;

    @Override
    public void onInitializeClient() {
        ArmorRenderers.register(
            GeoArmorRenderer.of(
                NeverAlone.id("geo/dark_amethyst.geo.json"),
                NeverAlone.id("textures/armor/dark_amethyst.png"))
                .radiant(),
            DarkAmethystItems.DARK_AMETHYST_HELMET,
            DarkAmethystItems.DARK_AMETHYST_CHESTPLATE,
            DarkAmethystItems.DARK_AMETHYST_LEGGINGS,
            DarkAmethystItems.DARK_AMETHYST_BOOTS);

        ArmorRenderers.register(
            GeoArmorRenderer.of(
                NeverAlone.id("geo/rhinestone.geo.json"),
                NeverAlone.id("textures/armor/rhinestone.png"))
                .glow(),
            RhinestoneItems.RHINESTONE_TIARA,
            RhinestoneItems.RHINESTONE_CHESTPLATE,
            RhinestoneItems.RHINESTONE_LEGGINGS,
            RhinestoneItems.RHINESTONE_BOOTS);

        ArmorRenderers.register(
            GeoArmorRenderer.of(
                NeverAlone.id("geo/emerald_warden.geo.json"),
                NeverAlone.id("textures/armor/emerald_warden.png"))
                .glow(),
            EmeraldWardenItems.EMERALD_WARDEN_HELMET,
            EmeraldWardenItems.EMERALD_WARDEN_CHESTPLATE,
            EmeraldWardenItems.EMERALD_WARDEN_LEGGINGS,
            EmeraldWardenItems.EMERALD_WARDEN_BOOTS);

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
            && client.gui.screen() == null && !client.isPaused();
        if (!playable) { wasInWorld = false; return; }
        if (!wasInWorld) { wasInWorld = true; return; }
        if (scareTicks > 0) { scareTicks--; return; }
        if (delayedEchoTicks > 0 && --delayedEchoTicks == 0) {
            scareTicks = ThreadLocalRandom.current().nextInt(3, 7);
            client.getSoundManager().play(SimpleSoundInstance.forUI(SOUNDS[scareIndex], 1.18F, 0.72F));
            return;
        }
        if (fakeOutTicks > 0) { fakeOutTicks--; return; }
        if (--ticksUntilScare <= 0) triggerScare(client);
    }

    private void triggerScare(Minecraft client) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        // About one in six scheduled scares is a fake-out: a tiny ominous delay, then usually nothing.
        if (rng.nextInt(100) < 16) {
            fakeOutTicks = rng.nextInt(4, 10);
            if (rng.nextInt(100) < 35) ticksUntilScare = rng.nextInt(20 * 10, 20 * 31);
            else resetTimer();
            return;
        }

        int previous = scareIndex;
        do {
            scareIndex = ThreadLocalRandom.current().nextInt(IMAGES.length);
        } while (IMAGES.length > 1 && scareIndex == previous);
        scareTicks = ThreadLocalRandom.current().nextInt(11, 19);
        float pitch = ThreadLocalRandom.current().nextFloat(0.92F, 1.09F);
        client.getSoundManager().play(SimpleSoundInstance.forUI(SOUNDS[scareIndex], pitch, 1.75F));
        // Very rarely, the same face flashes back briefly after the player thinks the scare is over.
        if (rng.nextInt(100) < 7) delayedEchoTicks = rng.nextInt(20 * 2, 20 * 6);
        resetTimer();
    }

    private void resetTimer() {
        ticksUntilScare = ThreadLocalRandom.current().nextInt(MIN_DELAY_TICKS, MAX_DELAY_TICKS + 1);
    }
}
