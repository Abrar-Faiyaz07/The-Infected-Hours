package com.theinfectedhour.exceptions;

/** Base unchecked exception for game-specific failures. */
public class GameException extends RuntimeException {

    public GameException(String message) {
        super(message);
    }

    public GameException(String message, Throwable cause) {
        super(message, cause);
    }
}
