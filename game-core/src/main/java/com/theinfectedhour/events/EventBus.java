package com.theinfectedhour.events;

import com.theinfectedhour.events.listeners.GameEventListener;

/** Singleton Observer-pattern bus: systems publish GameEvents and react to them without coupling to each other. */
public final class EventBus {

    private static EventBus instance;

    private EventBus() {
    }

    /** Sole accessor for the single EventBus instance (Singleton — Architecture.md §6). */
    public static synchronized EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    public void subscribe(Class<? extends GameEvent> eventType, GameEventListener listener) {
        // TODO: implement
    }

    public void unsubscribe(Class<? extends GameEvent> eventType, GameEventListener listener) {
        // TODO: implement
    }

    public void publish(GameEvent event) {
        // TODO: implement
    }
}
