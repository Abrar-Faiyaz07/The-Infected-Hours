package com.infectedhour.shared.network;

/**
 * Host's reply to a successful join. Carries the backend URL so BOTH
 * laptops talk to the same Spring Boot instance (TRD §6, App Flow §4).
 */
public class JoinAccept {
    public String backendUrl;
    public String hostDisplayName;
    public CharacterType assignedCharacter;

    public JoinAccept() {
    }

    public JoinAccept(String backendUrl, String hostDisplayName, CharacterType assignedCharacter) {
        this.backendUrl = backendUrl;
        this.hostDisplayName = hostDisplayName;
        this.assignedCharacter = assignedCharacter;
    }
}
