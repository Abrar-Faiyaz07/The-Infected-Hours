package com.infectedhour.backend.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** `player` table — Backend Schema §3. */
@Entity
@Table(name = "player")
public class Player {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 24)
    private String username;

    @Column(nullable = false, length = 32)
    private String displayName;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant lastLoginAt;

    protected Player() {
        // JPA
    }

    public Player(String username, String displayName, String passwordHash) {
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void recordLogin() {
        this.lastLoginAt = Instant.now();
    }
}
