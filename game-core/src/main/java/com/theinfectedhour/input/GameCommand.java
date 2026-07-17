package com.theinfectedhour.input;

/** Command-pattern contract: a player intent as a first-class, loggable, replayable object (Architecture.md §6). */
public interface GameCommand {

    void execute();

    void undo();
}
