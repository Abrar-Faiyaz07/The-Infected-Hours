package com.infectedhour.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infectedhour.shared.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full happy-path integration test for the single-transaction
 * POST /matches/{id}/complete endpoint (TRD §10): register a host player,
 * create a SOLO match, complete it, then verify the SaveState was updated
 * (Backend Schema §7 business rule 1 & 3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatchCompleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullMatchCompleteFlowUpdatesSaveState() throws Exception {
        // 1. Register + login host
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("host_integration", "Host", "pass1234"))))
                .andExpect(status().isCreated());

        String loginBody = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("host_integration", "pass1234"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginBody);
        String token = loginJson.get("token").asText();
        UUID hostId = UUID.fromString(loginJson.get("player").get("id").asText());

        // 2. Create a SOLO match
        MatchCreateRequest createRequest = new MatchCreateRequest("SOLO",
                List.of(new MatchCreateRequest.Participant(hostId, "ELRIC")));

        String createBody = mockMvc.perform(post("/api/v1/matches")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID matchId = UUID.fromString(objectMapper.readTree(createBody).get("matchId").asText());

        // 3. Complete the match — business rule 1: single transaction updates save + (for COOP) leaderboard
        MatchCompleteRequest completeRequest = new MatchCompleteRequest(
                "VICTORY", 3,
                List.of(new ParticipantStats(hostId, "ELRIC", 500, 4, 2, 3, 3, 2, 0, 0)),
                List.of(new SaveUpdate(hostId, 3, 4, 1800))
        );

        mockMvc.perform(post("/api/v1/matches/{id}/complete", matchId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk());

        // 4. Verify SaveState reflects the completed match (highest_level_unlocked, business rule 3)
        mockMvc.perform(get("/api/v1/players/me/save")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.highestLevelUnlocked").value(3))
                .andExpect(jsonPath("$.storyProgress").value(4));
    }
}
