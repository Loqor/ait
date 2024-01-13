package mdteam.ait.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import mdteam.ait.AITMod;
import mdteam.ait.network.ServerAITNetworkManager;
import mdteam.ait.tardis.Tardis;
import mdteam.ait.tardis.handler.properties.PropertiesHandler;
import mdteam.ait.tardis.wrapper.server.manager.ServerTardisManager;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static mdteam.ait.core.commands.TeleportInteriorCommand.TARDIS_SUGGESTION;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

// TEMPORARY - REMOVE LATER
public class ToggleSiegeModeCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal(AITMod.MOD_ID)
                .then(literal("toggle-siege-mode").requires(source -> source.hasPermissionLevel(2))
                        .then(argument("tardis", UuidArgumentType.uuid()).suggests(TARDIS_SUGGESTION)
                                .executes(ToggleSiegeModeCommand::runCommand))));
    }

    private static int runCommand(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity source = context.getSource().getPlayer();
        Tardis tardis = ServerTardisManager.getInstance().getTardis(UuidArgumentType.getUuid(context, "tardis"));

        if (tardis == null || source == null) return 0;

        PropertiesHandler.set(tardis.getHandlers().getProperties(), PropertiesHandler.SIEGE_MODE, !PropertiesHandler.getBool(tardis.getHandlers().getProperties(), PropertiesHandler.SIEGE_MODE));

        tardis.markDirty();

        source.sendMessage(Text.literal("Siege Mode set to: " + PropertiesHandler.getBool(tardis.getHandlers().getProperties(), PropertiesHandler.SIEGE_MODE)), true);
        ServerAITNetworkManager.sendTardisSiegeModeUpdate(tardis, PropertiesHandler.getBool(tardis.getHandlers().getProperties(), PropertiesHandler.SIEGE_MODE));

        return Command.SINGLE_SUCCESS;
    }
}
