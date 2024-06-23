package loqor.ait.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import loqor.ait.AITMod;
import loqor.ait.core.commands.argument.IdentifierWildcardArgumentType;
import loqor.ait.core.commands.argument.TardisArgumentType;
import loqor.ait.core.data.Wildcard;
import loqor.ait.core.data.base.Identifiable;
import loqor.ait.core.data.base.Nameable;
import loqor.ait.registry.impl.DesktopRegistry;
import loqor.ait.registry.impl.console.variant.ConsoleVariantRegistry;
import loqor.ait.registry.impl.exterior.ExteriorVariantRegistry;
import loqor.ait.registry.unlockable.Unlockable;
import loqor.ait.registry.unlockable.UnlockableRegistry;
import loqor.ait.tardis.wrapper.server.ServerTardis;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class UnlockCommand {

    public static final SuggestionProvider<ServerCommandSource> CONSOLE_SUGGESTION = (context, builder) -> IdentifierWildcardArgumentType.suggestWildcardIds(builder, ConsoleVariantRegistry.getInstance());
    public static final SuggestionProvider<ServerCommandSource> DESKTOP_SUGGESTION = (context, builder) -> IdentifierWildcardArgumentType.suggestWildcardIds(builder, DesktopRegistry.getInstance());
    public static final SuggestionProvider<ServerCommandSource> EXTERIOR_SUGGESTION = (context, builder) -> IdentifierWildcardArgumentType.suggestWildcardIds(builder, ExteriorVariantRegistry.getInstance());

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal(AITMod.MOD_ID)
                .then(literal("unlock").requires(source -> source.hasPermissionLevel(2))
                        .then(argument("tardis", TardisArgumentType.tardis())
                                .then(literal("console").then(argument("console", IdentifierWildcardArgumentType.wildcard())
                                        .suggests(CONSOLE_SUGGESTION).executes(UnlockCommand::unlockConsole)))
                                .then(literal("desktop").then(argument("desktop", IdentifierWildcardArgumentType.wildcard())
                                        .suggests(DESKTOP_SUGGESTION).executes(UnlockCommand::unlockDesktop)))
                                .then(literal("exterior").then(argument("exterior", IdentifierWildcardArgumentType.wildcard())
                                        .suggests(EXTERIOR_SUGGESTION).executes(UnlockCommand::unlockExterior)))
                        ))
        );
    }

    private static <T extends Identifiable & Unlockable & Nameable> int unlock(
            CommandContext<ServerCommandSource> context, String type, Wildcard<T> wildcard, UnlockableRegistry<T> registry
    ) {
        ServerCommandSource source = context.getSource();
        ServerTardis tardis = TardisArgumentType.getTardis(context, "tardis");

        if (wildcard.isPresent()) {
            T t = wildcard.get();
            tardis.stats().unlock(t);

            source.sendMessage(Text.translatableWithFallback("command.ait.unlock.some",
                    "Granted [%s] %s %s", tardis.getUuid(), t.name(), type)
            );

            return Command.SINGLE_SUCCESS;
        }

        registry.unlockAll(tardis);
        source.sendMessage(Text.translatableWithFallback("command.ait.unlock.all",
                "Granted [%s] every %s", tardis.getUuid(), type)
        );

        return Command.SINGLE_SUCCESS;
    }

    private static int unlockConsole(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return unlock(context, "console",
                IdentifierWildcardArgumentType.getConsoleVariantArgument(
                        context, "console"
                ), ConsoleVariantRegistry.getInstance()
        );
    }

    private static int unlockDesktop(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return unlock(context, "desktop",
                IdentifierWildcardArgumentType.getDesktopArgument(
                        context, "desktop"
                ), DesktopRegistry.getInstance()
        );
    }

    private static int unlockExterior(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return unlock(context, "exterior variant",
                IdentifierWildcardArgumentType.getExteriorVariantArgument(
                        context, "exterior"
                ), ExteriorVariantRegistry.getInstance()
        );
    }
}
