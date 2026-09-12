package com.infectedhour.backend.entity;

import jakarta.persistence.*;

import java.util.UUID;

/** `match_participant` table — Backend Schema §3. UNIQUE(match_id, player_id). */
@Entity
@Table(name = "match_participant", uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "player_id"}))
public class MatchParticipant {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    /**
     * Backtick-quoted: CHARACTER is a reserved word in MySQL 8, so unquoted DDL
     * fails with "error in your SQL syntax ... near 'character varchar(8)'".
     * H2's MySQL mode accepts it, which is why the tests passed.
     */
    @Column(name = "`character`", nullable = false, length = 8)
    private String character; // ELRIC | JANE

    private int damageDealt;
    private int villagersRescued;
    private int samplesCollected;
    private int medicineDelivered;
    private int barricadesPlaced;
    private int sanitationsDone;
    private int timesDowned;
    private int revivesDone;

    protected MatchParticipant() {
        // JPA
    }

    public MatchParticipant(Match match, Player player, String character) {
        this.match = match;
        this.player = player;
        this.character = character;
    }

    public UUID getId() {
        return id;
    }

    public Match getMatch() {
        return match;
    }

    public Player getPlayer() {
        return player;
    }

    public String getCharacter() {
        return character;
    }

    public void applyStats(int damageDealt, int villagersRescued, int samplesCollected,
                            int medicineDelivered, int barricadesPlaced, int sanitationsDone,
                            int timesDowned, int revivesDone) {
        this.damageDealt = damageDealt;
        this.villagersRescued = villagersRescued;
        this.samplesCollected = samplesCollected;
        this.medicineDelivered = medicineDelivered;
        this.barricadesPlaced = barricadesPlaced;
        this.sanitationsDone = sanitationsDone;
        this.timesDowned = timesDowned;
        this.revivesDone = revivesDone;
    }
}
