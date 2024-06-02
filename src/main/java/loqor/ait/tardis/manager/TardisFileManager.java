package loqor.ait.tardis.manager;

import com.google.gson.Gson;
import loqor.ait.AITMod;
import loqor.ait.tardis.Tardis;
import loqor.ait.tardis.TardisManager;
import loqor.ait.tardis.wrapper.server.ServerTardis;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.BiFunction;

public class TardisFileManager<T extends Tardis> {

    private final Class<T> clazz;

    public TardisFileManager(Class<T> clazz) {
        this.clazz = clazz;
    }

    public void delete(MinecraftServer server, UUID uuid) {
        try {
            Files.deleteIfExists(TardisFileManager.getSavePath(server, uuid, "json"));
        } catch (IOException e) {
            AITMod.LOGGER.error("Failed to delete TARDIS " + uuid, e);
        }
    }

    private static Path getRootSavePath(Path root) {
        return root.resolve(".ait");
    }

    public static Path getRootSavePath(MinecraftServer server) {
        return TardisFileManager.getRootSavePath(server.getSavePath(WorldSavePath.ROOT));
    }

    private static Path getSavePath(MinecraftServer server, UUID uuid, String suffix) throws IOException {
        Path result = TardisFileManager.getRootSavePath(server).resolve(uuid.toString() + "." + suffix);
        Files.createDirectories(result.getParent());

        return result;
    }

    public T loadTardis(MinecraftServer server, TardisManager<T, ?> manager, UUID uuid, TardisLoader<T> function) {
        try {
            Path file = TardisFileManager.getSavePath(server, uuid, "json");
            String json = Files.readString(file);

            T tardis = function.apply(manager.getFileGson(), json, this.clazz);
            manager.getLookup().put(tardis.getUuid(), tardis);
            return tardis;
        } catch (IOException e) {
            AITMod.LOGGER.warn("Failed to load tardis with uuid {}!", uuid);
            AITMod.LOGGER.warn(e.getMessage());
        }

        return null;
    }

    public void saveTardis(MinecraftServer server, TardisManager<T, ?> manager) {
        for (T tardis : manager.getLookup().values()) {
            this.saveTardis(server, manager, tardis);
        }
    }

    public void saveTardis(MinecraftServer server, TardisManager<T, ?> manager, T tardis) {
        try {
            Path savePath = TardisFileManager.getSavePath(server, tardis.getUuid(), "json");
            Files.writeString(savePath, manager.getFileGson().toJson(tardis, ServerTardis.class));
        } catch (IOException e) {
            AITMod.LOGGER.warn("Couldn't save TARDIS " + tardis.getUuid(), e);
        }
    }

    @FunctionalInterface
    public interface TardisLoader<T> {
        T apply(Gson gson, String name, Class<T> clazz);
    }
}
