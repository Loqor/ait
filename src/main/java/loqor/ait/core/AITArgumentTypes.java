package loqor.ait.core;

import com.mojang.brigadier.arguments.ArgumentType;
import loqor.ait.AITMod;
import loqor.ait.core.commands.argument.*;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public class AITArgumentTypes {

    public static void register() {
        register("tardis", TardisArgumentType.class, TardisArgumentType::tardis);
        register("wildcard_resource_location", IdentifierWildcardArgumentType.class, IdentifierWildcardArgumentType::wildcard);
        register("permission", PermissionArgumentType.class, PermissionArgumentType::permission);
        register("json", JsonElementArgumentType.class, JsonElementArgumentType::jsonElement);
        register("ground_search", GroundSearchArgumentType.class, GroundSearchArgumentType::groundSearch);
    }

    private static <T extends ArgumentType<?>> void register(String name, Class<T> t, Supplier<T> supplier) {
        ArgumentTypeRegistry.registerArgumentType(new Identifier(AITMod.MOD_ID, name), t, ConstantArgumentSerializer.of(supplier));
    }
}
