package com.theinfectedhour.events.listeners;

import com.theinfectedhour.events.GameEvent;

/** Observer contract for reacting to GameEvents published on the EventBus. */
@FunctionalInterface
public interface GameEventListener {

    void onEvent(GameEvent event);
}
