package com.neveralone.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.world.item.CreativeModeTabs;

@Mod(NeverAloneForge.ID)
public final class NeverAloneForge {
    public static final String ID = "never_alone";
    public NeverAloneForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        NAItems.ITEMS.register(modBus);
        NALootModifiers.CODECS.register(modBus);
        modBus.addListener(this::creativeContents);
        MinecraftForge.EVENT_BUS.addListener(NACommands::register);
        MinecraftForge.EVENT_BUS.addListener(NADirector::tick);
        MinecraftForge.EVENT_BUS.addListener(NADirector::onLogout);
    }
    private void creativeContents(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey().equals(CreativeModeTabs.COMBAT)) {
            NAItems.ARMOR.values().forEach(parts -> parts.values().forEach(item -> e.accept(item.get())));
        }
        if (e.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            NAItems.MATERIALS.values().forEach(item -> e.accept(item.get()));
        }
    }
}
