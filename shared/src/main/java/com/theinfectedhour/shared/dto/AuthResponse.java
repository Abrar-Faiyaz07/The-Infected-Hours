package com.theinfectedhour.shared.dto;

import java.io.Serializable;

/** Backend reply to authentication: session token and account info. */
public class AuthResponse implements Serializable {

    public String token;
    public String username;
}
