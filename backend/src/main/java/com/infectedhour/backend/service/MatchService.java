package com.infectedhour.backend.service;

import com.infectedhour.backend.entity.*;
import com.infectedhour.backend.repository.*;
import com.infectedhour.shared.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * POST /matches, /matches/{id}/level-result, /matches/{id}/complete
 * (Backend Schema §4). {@link #completeMatch} is the single-transaction
 * source of truth described in business rule 1 (§7): updates saves for
 * ALL participants, upserts leaderboard bests, all in one transaction.
 */
@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository participantRepository;
    private final LevelResultRepository levelResultRepository;
    private final LeaderboardEntryRepository leaderboardRepository;
    private final PlayerRepository playerRepository;
    private final SaveStateRepository saveStateRepository;

    public MatchService(MatchRepository matchRepository, MatchParticipantRepository participantRepository,
                         LevelResultRepository levelResultRepository, LeaderboardEntryRepository leaderboardRepository,
                         PlayerRepository playerRepository, SaveStateRepository saveStateRepository) {
        this.matchRepository = matchRepository;
        this.participantRepository = participantRepository;
        this.levelResultRepository = levelResultRepository;
        this.leaderboardRepository = leaderboardRepository;
        this.playerRepository = playerRepository;
        this.saveStateRepository = saveStateRepository;
    }

    @Transactional
    public UUID createMatch(UUID hostPlayerId, MatchCreateRequest request) {
        Player host = playerRepository.findById(hostPlayerId)
                .orElseThrow(() -> new IllegalArgumentException("Host player not found"));

        Match match = matchRepository.save(new Match(host, request.mode()));

        for (MatchCreateRequest.Participant p : request.participants()) {
            Player player = playerRepository.findById(p.playerId())
                    .orElseThrow(() -> new IllegalArgumentException("Participant not found: " + p.playerId()));
            participantRepository.save(new MatchParticipant(match, player, p.character()));
        }

        return match.getId();
    }

    /** Idempotent per level_number (Backend Schema §4) — upsert instead of always inserting. */
    @Transactional
    public void submitLevelResult(UUID matchId, LevelResultRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        levelResultRepository.findByMatchIdAndLevelNumber(matchId, request.levelNumber())
                .ifPresentOrElse(
                        existing -> {
                            // TEAMMATE TASK (backend): on a re-submit for the
                            // same level (a retry): keep cleared=true if either
                            // is true, keep the BETTER clearTimeSec, sum the
                            // retries. Add an update method on LevelResult.
                        },
                        () -> levelResultRepository.save(new LevelResult(match, request.levelNumber(),
                                request.cleared(), request.clearTimeSec(), request.finalContaminationPct(), request.retries()))
                );
    }

    @Transactional
    public void completeMatch(UUID matchId, MatchCompleteRequest request) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        match.complete(request.result(), request.finalLevelReached());

        for (ParticipantStats stats : request.participants()) {
            MatchParticipant participant = participantRepository.findByMatchIdAndPlayerId(matchId, stats.playerId())
                    .orElseThrow(() -> new IllegalArgumentException("Participant not found: " + stats.playerId()));
            participant.applyStats(stats.damageDealt(), stats.villagersRescued(), stats.samplesCollected(),
                    stats.medicineDelivered(), stats.barricadesPlaced(), stats.sanitationsDone(),
                    stats.timesDowned(), stats.revivesDone());
        }

        // Business rule 5: aborted matches store stats but never touch leaderboards or level unlocks.
        boolean aborted = "ABORTED".equals(request.result());

        for (SaveUpdate saveUpdate : request.saves()) {
            SaveState save = saveStateRepository.findByPlayerId(saveUpdate.playerId())
                    .orElseThrow(() -> new IllegalArgumentException("Save state not found: " + saveUpdate.playerId()));
            if (!aborted) {
                save.applyUpdate(saveUpdate.highestLevelUnlocked(), saveUpdate.storyProgress(), saveUpdate.playtimeDeltaSec());
            } else {
                // still track playtime even when aborted
                save.applyUpdate(save.getHighestLevelUnlocked(), save.getStoryProgress(), saveUpdate.playtimeDeltaSec());
            }
        }

        if (!aborted) {
            upsertLeaderboardEntries(match, request);
        }
    }

    private void upsertLeaderboardEntries(Match match, MatchCompleteRequest request) {
        // ================ TEAMMATE TASK: LEADERBOARD UPSERT ================
        // TODO(backend): the ONLY missing backend logic. Steps:
        //  1. Load this match's LevelResults — add to LevelResultRepository:
        //       List<LevelResult> findByMatchId(UUID matchId);
        //  2. For each CLEARED level n with clearTimeSec != null:
        //       board "FASTEST_L" + n        -> value = clearTimeSec
        //       board "LOWEST_CONTAM_L" + n  -> value = finalContaminationPct
        //     and for level 3 additionally:
        //       board "BOSS_TIME"            -> value = clearTimeSec
        //  3. For each participant's player:
        //       leaderboardRepository.findByPlayerIdAndBoard(playerId, board)
        //           .ifPresentOrElse(
        //               e -> e.replaceIfBetter(value, match),          // rule 2
        //               () -> leaderboardRepository.save(
        //                   new LeaderboardEntry(player, board, value, match)));
        //  4. Rule 4: SOLO matches DO write leaderboards. This runs inside
        //     completeMatch's transaction — no extra @Transactional needed.
        //  5. Verify with the MatchCompleteIntegrationTest: extend it to
        //     assert a leaderboard row exists after completion.
        // ===================================================================
    }

    /** GET /players/me/matches?limit=20 (Backend Schema §4, Profile screen). */
    public List<MatchHistoryEntryDto> getHistory(UUID playerId, int limit) {
        return matchRepository.findByHostPlayerIdOrderByStartedAtDesc(playerId).stream()
                .limit(limit)
                .map(m -> new MatchHistoryEntryDto(
                        m.getId(), m.getMode(), m.getResult(),
                        m.getFinalLevelReached() != null ? m.getFinalLevelReached() : 0,
                        m.getStartedAt().toString(),
                        m.getEndedAt() != null ? m.getEndedAt().toString() : null))
                .toList();
        // TEAMMATE TASK (backend, small): this only returns matches where the
        // player was the HOST. Also include matches they JOINED — add to
        // MatchParticipantRepository:
        //   List<MatchParticipant> findByPlayerIdOrderByMatch_StartedAtDesc(UUID playerId);
        // then merge both lists, de-dupe by match id, sort desc, apply limit.
    }
}
