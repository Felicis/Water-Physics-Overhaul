package net.skds.wpo.events;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;
import net.skds.wpo.WPO;
import net.skds.wpo.commands.ExportFluidStatesCommand;

@Mod.EventBusSubscriber(modid = WPO.MOD_ID)
public class OtherEvents {
    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        LOGGER.debug("OtherEvents:onCommandsRegister");
//        new ExportFluidStatesCommand(event.getDispatcher());
        ExportFluidStatesCommand.register(event.getDispatcher());
        ConfigCommand.register(event.getDispatcher());
    }
}
