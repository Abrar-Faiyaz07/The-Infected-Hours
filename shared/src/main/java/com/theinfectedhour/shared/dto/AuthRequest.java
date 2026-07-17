package com.theinfectedhour.shared.dto;

import java.io.Serializable;

/** Login/registration payload sent from the game client to the backend. */
public class AuthRequest implements Serializable {

    public String username;
    public String password;
}
