package com.infectedhour.shared.network;

/**
 * Host's reply to a successful join. Carries the backend URL so BOTH
 * laptops talk to the same Spring Boot instance (TRD §6, App Flow §4),
 * and the player id the host filed this connection under — the client
 * needs it to pick its OWN entry out of {@link WorldSnapshot#players}
 * (camera follow, HUD), since this build renders every entity from the
 * snapshot with no client-side prediction.
 */
public class JoinAccept {
    public String backendUrl;
    public String hostDisplayName;
    public CharacterType assignedCharacter;
    public String assignedPlayerId;
    public MatchMode matchMode;

    public JoinAccept() {
    }

    public JoinAccept(String backendUrl, String hostDisplayName, CharacterType assignedCharacter,
                      String assignedPlayerId, MatchMode matchMode) {
        this.backendUrl = backendUrl;
        this.hostDisplayName = hostDisplayName;
        this.assignedCharacter = assignedCharacter;
        this.assignedPlayerId = assignedPlayerId;
        this.matchMode = matchMode;
    }
}
