package dev.loqor.portal;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Shared identifiers for the two directions of a TARDIS's "bigger on the inside" portal.
 * <p>
 * Each TARDIS streams two shadow worlds to viewers, distinguished only by the {@code UUID} that tags their
 * packets (see {@link WrappedPacketS2CPacket} / {@link PortalInitS2CPacket}):
 * <ul>
 *   <li><b>exterior stream</b> — id = the TARDIS UUID; proxy parked at the exterior; shown through the
 *       <em>interior</em> door to players inside.</li>
 *   <li><b>interior stream</b> — id = {@link #interiorId(UUID)}; proxy parked at the interior door; shown through
 *       the <em>exterior</em> door to players outside.</li>
 * </ul>
 * The derivation is deterministic so the server and client agree on the id without sending it.
 */
public final class Portals {

    private Portals() {
    }

    /** Deterministic, collision-resistant portal id for a TARDIS's interior stream (the exterior→interior view). */
    public static UUID interiorId(UUID tardis) {
        return UUID.nameUUIDFromBytes(("ait:boti:interior:" + tardis).getBytes(StandardCharsets.UTF_8));
    }
}
