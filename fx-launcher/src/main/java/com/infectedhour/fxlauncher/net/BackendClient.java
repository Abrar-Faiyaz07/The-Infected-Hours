package com.infectedhour.fxlauncher.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infectedhour.fxlauncher.state.SessionState;
import com.infectedhour.shared.dto.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Wraps calls to :backend's REST API (Backend Schema §4) using Java's
 * built-in HttpClient + Jackson (TRD §1). All calls are async
 * (CompletableFuture) per TRD §9: "gameplay never blocks on HTTP".
 *
 * TEAMMATE TASK (fx): backend-down resilience (App Flow par.6):
 *  - On a failed WRITE (POST/PUT), serialize the request JSON to
 *    ~/.infectedhour/pending/[timestamp].json instead of losing it.
 *  - On launcher start, retry every pending file with backoff; delete on
 *    success. READ failures can just surface to the calling view.
 *  Currently this class propagates failures to the caller.
 */
public class BackendClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private String baseUrl() {
        return SessionState.get().getBackendUrl() + "/api/v1";
    }

    public CompletableFuture<HealthStatus> health() {
        return getAsync("/health", HealthStatus.class, false);
    }

    public CompletableFuture<Void> register(RegisterRequest request) {
        return postAsync("/auth/register", request, Void.class, false);
    }

    public CompletableFuture<LoginResponse> login(LoginRequest request) {
        return postAsync("/auth/login", request, LoginResponse.class, false)
                .thenApply(response -> {
                    SessionState.get().setJwtToken(response.token());
                    SessionState.get().setCurrentPlayer(response.player());
                    return response;
                });
    }

    public CompletableFuture<PlayerDto> getMe() {
        return getAsync("/players/me", PlayerDto.class, true);
    }

    public CompletableFuture<SaveStateDto> getMySave() {
        return getAsync("/players/me/save", SaveStateDto.class, true);
    }

    public CompletableFuture<Void> putMySave(SaveStateDto save) {
        return putAsync("/players/me/save", save, Void.class, true);
    }

    public CompletableFuture<List<LeaderboardEntryDto>> getLeaderboard(String board, int limit) {
        return getAsync("/leaderboards/" + board + "?limit=" + limit, List.class, true)
                .thenApply(raw -> mapper.convertValue(raw, mapper.getTypeFactory()
                        .constructCollectionType(List.class, LeaderboardEntryDto.class)));
    }

    // --- low-level helpers ---

    private <T> CompletableFuture<T> getAsync(String path, Class<T> responseType, boolean authed) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + path)).GET();
        applyAuth(builder, authed);
        return send(builder, responseType);
    }

    private <T> CompletableFuture<T> postAsync(String path, Object body, Class<T> responseType, boolean authed) {
        return sendWithBody("POST", path, body, responseType, authed);
    }

    private <T> CompletableFuture<T> putAsync(String path, Object body, Class<T> responseType, boolean authed) {
        return sendWithBody("PUT", path, body, responseType, authed);
    }

    private <T> CompletableFuture<T> sendWithBody(String method, String path, Object body, Class<T> responseType, boolean authed) {
        try {
            String json = mapper.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl() + path))
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json));
            applyAuth(builder, authed);
            return send(builder, responseType);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private void applyAuth(HttpRequest.Builder builder, boolean authed) {
        if (authed && SessionState.get().getJwtToken() != null) {
            builder.header("Authorization", "Bearer " + SessionState.get().getJwtToken());
        }
    }

    private <T> CompletableFuture<T> send(HttpRequest.Builder builder, Class<T> responseType) {
        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 400) {
                        throw new RuntimeException("Backend error " + response.statusCode() + ": " + response.body());
                    }
                    try {
                        if (responseType == Void.class || response.body().isBlank()) return null;
                        return mapper.readValue(response.body(), responseType);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
