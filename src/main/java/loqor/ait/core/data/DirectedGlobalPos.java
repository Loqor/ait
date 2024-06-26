package loqor.ait.core.data;

import com.google.gson.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Function;

public class DirectedGlobalPos {

    public static final Codec<DirectedGlobalPos> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    World.CODEC.fieldOf("dimension")
                            .forGetter(DirectedGlobalPos::getDimension),
                    BlockPos.CODEC.fieldOf("pos")
                            .forGetter(DirectedGlobalPos::getPos),
                    Codec.BYTE.fieldOf("rotation")
                            .forGetter(DirectedGlobalPos::getRotation)
            ).apply(instance, DirectedGlobalPos::create));

    private final RegistryKey<World> dimension;
    private final BlockPos pos;
    private final byte rotation;

    private DirectedGlobalPos(RegistryKey<World> dimension, BlockPos pos, byte rotation) {
        this.dimension = dimension;
        this.pos = pos;

        this.rotation = rotation;
    }

    public DirectedGlobalPos pos(int x, int y, int z) {
        return this.pos(new BlockPos(x, y, z));
    }

    public DirectedGlobalPos pos(BlockPos pos) {
        return DirectedGlobalPos.create(this.dimension, pos, this.rotation);
    }

    public DirectedGlobalPos offset(int x, int y, int z) {
        return DirectedGlobalPos.create(this.dimension, this.pos.add(x, y, z), this.rotation);
    }

    public DirectedGlobalPos apply(Function<Integer, Integer> func) {
        return DirectedGlobalPos.create(this.dimension, new BlockPos(
                func.apply(this.pos.getX()),
                func.apply(this.pos.getY()),
                func.apply(this.pos.getZ())
        ), this.rotation);
    }

    public DirectedGlobalPos world(RegistryKey<World> world) {
        return DirectedGlobalPos.create(world, this.pos, this.rotation);
    }

    public static DirectedGlobalPos create(RegistryKey<World> dimension, BlockPos pos, byte rotation) {
        return new DirectedGlobalPos(dimension, pos, rotation);
    }

    public RegistryKey<World> getDimension() {
        return this.dimension;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public byte getRotation() {
        return this.rotation;
    }

    public Vec3i getVector() {
        return switch (this.rotation) {
            default -> new Vec3i(0, 0, 0);
            case 0 -> Direction.NORTH.getVector();
            case 1, 2, 3 -> Direction.NORTH.getVector().add(Direction.EAST.getVector());
            case 4 -> Direction.EAST.getVector();
            case 5, 6, 7 -> Direction.EAST.getVector().add(Direction.SOUTH.getVector());
            case 8 -> Direction.SOUTH.getVector();
            case 9, 10, 11 -> Direction.SOUTH.getVector().add(Direction.WEST.getVector());
            case 12 -> Direction.WEST.getVector();
            case 13, 14, 15 -> Direction.NORTH.getVector().add(Direction.SOUTH.getVector());
        };
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof DirectedGlobalPos globalPos))
            return false;

        return Objects.equals(this.dimension, globalPos.dimension)
                && Objects.equals(this.pos, globalPos.pos)
                && Objects.equals(this.rotation, globalPos.rotation);
    }

    public int hashCode() {
        return Objects.hash(this.dimension, this.pos, this.rotation);
    }

    public String toString() {
        return this.dimension + " " + this.pos + " " + this.rotation;
    }

    public void write(PacketByteBuf buf) {
        buf.writeRegistryKey(this.getDimension());
        buf.writeBlockPos(this.getPos());
        buf.writeByte(this.rotation);
    }

    public static DirectedGlobalPos read(PacketByteBuf buf) {
        RegistryKey<World> registryKey = buf.readRegistryKey(RegistryKeys.WORLD);
        BlockPos blockPos = buf.readBlockPos();
        byte rotation = buf.readByte();

        return DirectedGlobalPos.create(registryKey, blockPos, rotation);
    }

    public static Object serializer() {
        return new Serializer();
    }

    private static class Serializer implements JsonDeserializer<DirectedGlobalPos>, JsonSerializer<DirectedGlobalPos> {

        @Override
        public DirectedGlobalPos deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();

            RegistryKey<World> dimension = context.deserialize(obj.get("dimension"), RegistryKey.class);

            int x = obj.get("x").getAsInt();
            int y = obj.get("y").getAsInt();
            int z = obj.get("z").getAsInt();
            byte rotation = obj.get("rotation").getAsByte();

            return DirectedGlobalPos.create(dimension, new BlockPos(x, y, z), rotation);
        }

        @Override
        public JsonElement serialize(DirectedGlobalPos src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();

            result.addProperty("dimension", src.getDimension().getValue().toString());
            result.addProperty("x", src.getPos().getX());
            result.addProperty("y", src.getPos().getY());
            result.addProperty("z", src.getPos().getZ());
            result.addProperty("rotation", src.getRotation());

            return result;
        }
    }
}
