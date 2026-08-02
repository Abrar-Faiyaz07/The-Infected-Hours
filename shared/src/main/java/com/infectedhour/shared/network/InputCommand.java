package com.infectedhour.shared.network;

/**
 * Client → Host, sent at CLIENT_INPUT_SEND_HZ (30). Per TRD §5 this build has
 * NO client-side prediction — the client only ever sends intent, never
 * authoritative position.
 */
public class InputCommand {
    public long clientTick;
    public float moveX;
    public float moveY;
    public boolean attackPressed;
    public boolean interactPressed;
    public boolean interactHeld;
    public boolean abilityPressed;
    public int selectedInventorySlot; // 0-3
    public boolean dropPressed;

    public InputCommand() {
    }
}