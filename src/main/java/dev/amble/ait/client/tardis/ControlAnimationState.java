package dev.amble.ait.client.tardis;

import dev.amble.lib.client.bedrock.TargetedAnimationState;

/**
 * One console control's animation state, held by the console rather than by the control entity.
 *
 * <p>The renderer walks a console's control slots every frame and needs somewhere per slot to keep
 * animation progress, plus whether that control is on cooldown. Progress is presentation state and
 * lives here outright. Cooldown is server authoritative and reaches the client only as tracked data
 * on the control entity, so the entity pushes it in on its own tick.
 *
 * <p>Only ever populated client side, and nothing here is synced or saved. Deliberately not
 * annotated {@code @Environment(CLIENT)}: it is named in {@link
 * dev.amble.ait.core.blockentities.ConsoleBlockEntity}'s field and method signatures, which are
 * common code, so stripping it from the dedicated server would leave those descriptors
 * unresolvable.
 */
public final class ControlAnimationState {

    /**
     * How many ticks a set cooldown is trusted for without being re-confirmed.
     *
     * <p>Extends, by up to this many ticks, any cooldown whose release push did not land. A
     * control pushes its release explicitly, so a cooldown that ends while its console is loaded
     * ends exactly. This covers the cases where no push arrives at all: control entities have no
     * tracking range set and so default to five chunks while a console draws further, and a
     * control's offsets can put it in a neighbouring chunk from its console. Delays run as long as
     * 360000 ticks, so a flag left latched would hold a lever fully actuated for hours.
     */
    private static final int GRACE_TICKS = 3;

    private final TargetedAnimationState state = new TargetedAnimationState();

    private boolean cooldown;

    /**
     * Console age at the last push. Never read before a push, because {@link #cooldown(int)}
     * short-circuits on the flag first; -1 rather than MIN_VALUE so that if it ever is, the
     * subtraction stays sane.
     */
    private int pushedAtTick = -1;

    public TargetedAnimationState state() {
        return this.state;
    }

    /**
     * Called by the control entity that owns this slot, on its client tick, while it is on
     * cooldown and once when that ends.
     */
    public void push(boolean onDelay, int tick) {
        this.cooldown = onDelay;
        this.pushedAtTick = tick;
    }

    /** Whether this control is on cooldown, as far as a control still confirming it has said. */
    public boolean cooldown(int tick) {
        return this.cooldown && tick - this.pushedAtTick <= GRACE_TICKS;
    }

    /**
     * Advances progress toward the target.
     *
     * <p>{@code BedrockAnimation#apply} already ticks the state it is handed, so this is not needed
     * for a console being drawn. It keeps a console that is culled from the frustum advancing
     * anyway, which is what the control entities used to do.
     */
    public void tick() {
        this.state.tick();
    }
}
