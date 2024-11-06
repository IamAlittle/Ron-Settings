package com.iamalittle.ronsettings;

import com.mojang.logging.LogUtils;
import com.iamalittle.ronsettings.commands.RONModifyResourcesCommand;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RonSettingsMOD.MODID)
public class RonSettingsMOD {
    public static final String MODID = "ronsettings";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RonSettingsMOD() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("RonSettingsMOD setup initialized");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Registering commands");
        RONModifyResourcesCommand.register(event.getServer().getCommands().getDispatcher());
    }
}
