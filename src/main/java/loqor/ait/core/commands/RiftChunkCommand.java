package loqor.ait.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import loqor.ait.AITMod;
import loqor.ait.core.managers.RiftChunkManager;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class RiftChunkCommand {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal(AITMod.MOD_ID)
				.then(literal("rift_chunk").requires(source -> source.hasPermissionLevel(2))
						.then(literal("check")
								.then(argument("position", BlockPosArgumentType.blockPos())
										.executes(RiftChunkCommand::check)))
						.then(literal("get")
								.then(argument("position", BlockPosArgumentType.blockPos())
										.executes(RiftChunkCommand::get)))
						.then(literal("set")
								.then(argument("position", BlockPosArgumentType.blockPos())
										.then(argument("artron", IntegerArgumentType.integer())
												.executes(RiftChunkCommand::set)
										)))));
	}

	private static int check(CommandContext<ServerCommandSource> context) {
		BlockPos targetBlockPos = BlockPosArgumentType.getBlockPos(context, "position");
		ServerCommandSource source = context.getSource();

		boolean isARiftChunk = RiftChunkManager.isRiftChunk(targetBlockPos);
		Text isriftchunk = Text.translatable("message.ait.sonic.riftfound");
		Text notriftchunk = Text.translatable("message.ait.sonic.riftnotfound");

		source.sendMessage((isARiftChunk ? isriftchunk : notriftchunk));
		return 1;
	}

	private static int get(CommandContext<ServerCommandSource> context) {
		BlockPos targetBlockPos = BlockPosArgumentType.getBlockPos(context, "position");
		ServerCommandSource source = context.getSource();

		boolean isARiftChunk = RiftChunkManager.isRiftChunk(targetBlockPos);

		Text message = !isARiftChunk ? Text.translatable("command.ait.riftchunk.cannotgetlevel") :
				Text.translatable("command.ait.riftchunk.getlevel", RiftChunkManager.getArtronLevels(source.getWorld(), targetBlockPos));

		source.sendMessage(message);
		return 1;
	}

	private static int set(CommandContext<ServerCommandSource> context) {
		BlockPos targetBlockPos = BlockPosArgumentType.getBlockPos(context, "position");
		ServerCommandSource source = context.getSource();

		Text message;

		if (!RiftChunkManager.isRiftChunk(targetBlockPos)) {
			message = Text.translatable("command.ait.riftchunk.cannotsetlevel"); // This chunk is not a rift chunk, so you can't get the artron levels of it
		} else {
			Integer artron = IntegerArgumentType.getInteger(context, "artron");
			RiftChunkManager.setArtronLevels(source.getWorld(), targetBlockPos, artron);

			message = Text.translatable("command.ait.riftchunk.setlevel", artron);
		}

		source.sendMessage(message);
		return 1;
	}
}
