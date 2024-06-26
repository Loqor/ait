package loqor.ait.tardis.link.v2;

import loqor.ait.AITMod;
import loqor.ait.tardis.Tardis;
import loqor.ait.tardis.util.Disposable;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public class TardisRef implements Disposable {

    private final LoadFunc load;
    private UUID id;

    private Tardis cached;

    public TardisRef(UUID id, LoadFunc load) {
        this.id = id;
        this.load = load;

        AITMod.LOGGER.debug("Created new ref1 for " + id, new Throwable());
    }

    public TardisRef(Tardis tardis, LoadFunc load) {
        if (tardis != null)
            this.id = tardis.getUuid();

        this.load = load;
        this.cached = tardis;
        AITMod.LOGGER.debug("Created new ref2 for " + id, new Throwable());
    }

    public Tardis get() {
        if (this.cached != null && !this.cached.isAged())
            return this.cached;

        this.cached = this.load.apply(this.id);
        return this.cached;
    }

    public UUID getId() {
        return id;
    }

    public boolean isPresent() {
        return this.get() != null;
    }

    public boolean isEmpty() {
        return this.get() == null;
    }

    /**
     * @return the result of the function, {@literal null} otherwise.
     */
    public <T> T apply(Function<Tardis, T> consumer) {
        if (this.isPresent())
            return consumer.apply(this.cached);

        return null;
    }

    public void ifPresent(Consumer<Tardis> consumer) {
        if (this.isPresent())
            consumer.accept(this.cached);
    }

    @Override
    public void dispose() {
        this.cached = null;
    }

    public interface LoadFunc extends Function<UUID, Tardis> { }
}
