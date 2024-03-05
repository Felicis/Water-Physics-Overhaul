package net.skds.wpo;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.skds.wpo.client.ClientEvents;
import net.skds.wpo.config.WPOConfig;
import net.skds.wpo.network.PacketHandler;
import net.skds.wpo.registry.WPO_Entities;
import net.skds.wpo.registry.WPO_Blocks;
import net.skds.wpo.registry.WPO_Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("wpo")
public class WPO
{
    public static final String MOD_ID = "wpo";
    public static final String MOD_NAME = "Water Physics Overhaul";
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public static WPOEvents WPO_EVENTS = new WPOEvents();

    public WPO() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConfigLoad);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(WPO_EVENTS);
        MinecraftForge.EVENT_BUS.register(this);
      
        WPOConfig.init();
        WPO_Items.register();
        WPO_Blocks.register();
        WPO_Entities.register();
        PacketHandler.init();
    }
    

    private void setup(final FMLCommonSetupEvent event) {
    }

    public void onConfigLoad(ModConfig.ModConfigEvent event) {
        WPOConfig.SERVER.cacheBlockListConfigs();
    }

    private void doClientStuff(final FMLClientSetupEvent event) {  
        ClientEvents.setup(event);
    }
}
