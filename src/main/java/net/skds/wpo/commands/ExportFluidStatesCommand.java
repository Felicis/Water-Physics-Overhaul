package net.skds.wpo.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.skds.wpo.WPO;

public class ExportFluidStatesCommand implements Command<CommandSource> {
    @Override
    public int run(CommandContext<CommandSource> context) {
        WPO.LOGGER.debug("ExportFluidStatesCommand:run");
        CommandSource source = context.getSource();
        String message = StringArgumentType.getString(context, "message");

        // Execute your custom function here
        source.sendSuccess(new StringTextComponent("You said: " + message), true);

        return Command.SINGLE_SUCCESS;
    }

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        WPO.LOGGER.debug("ExportFluidStatesCommand:register");
        dispatcher.register(Commands.literal("exportFS")
                .then(Commands.argument("message", StringArgumentType.string())
                        .executes(new ExportFluidStatesCommand())));
    }
}
