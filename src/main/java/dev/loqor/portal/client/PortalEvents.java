package dev.loqor.portal.client;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface PortalEvents {

    Event<PortalUpdateEvent> UPDATE = EventFactory.createArrayBacked(PortalUpdateEvent.class, events -> (data) -> {
        for (PortalUpdateEvent event : events) {
            event.onPortalUpdate(data);
        }
    });

    @FunctionalInterface
    interface PortalUpdateEvent {
        void onPortalUpdate(PortalData data);
    }
}
