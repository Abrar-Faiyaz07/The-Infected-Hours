package com.infectedhour.backend.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregate reporting queries — the Profile screen and the end-of-match
 * summary.
 *
 * <p>These are deliberately raw SQL rather than JPA. Every query here collapses
 * many rows into a handful of numbers; expressing them through the entity model
 * would mean loading whole object graphs into memory and summing them in Java,
 * which is both slower and further from what is actually being asked. A
 * {@code SUM} belongs in the database.
 *
 * <p>Note the backticks around {@code `match`} — MATCH is a reserved word in
 * MySQL 8, so every reference to that table must be quoted.
 */
@Repository
public class PlayerStatsDao {

    private final JdbcTemplate jdbc;

    public PlayerStatsDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Career totals for the Profile screen, in one pass over the participant rows.
     *
     * <p>{@code COALESCE} guards the case of a player who has never finished a
     * match: {@code SUM} over zero rows returns NULL, not 0, and a null here
     * would surface as an empty stat on the profile card.
     */
    private static final String CAREER_TOTALS = """
            SELECT COALESCE(SUM(mp.damage_dealt), 0)        AS damage_dealt,
                   COALESCE(SUM(mp.villagers_rescued), 0)   AS villagers_rescued,
                   COALESCE(SUM(mp.samples_collected), 0)   AS samples_collected,
                   COALESCE(SUM(mp.medicine_delivered), 0)  AS medicine_delivered,
                   COALESCE(SUM(mp.barricades_placed), 0)   AS barricades_placed,
                   COALESCE(SUM(mp.sanitations_done), 0)    AS sanitations_done,
                   COALESCE(SUM(mp.times_downed), 0)        AS times_downed,
                   COALESCE(SUM(mp.revives_done), 0)        AS revives_done,
                   COUNT(*)                                 AS matches_played
              FROM match_participant mp
             WHERE mp.player_id = ?
            """;

    /** Win rate and match counts, split by outcome. */
    private static final String MATCH_OUTCOMES = """
            SELECT m.result       AS result,
                   COUNT(*)       AS total
              FROM match_participant mp
              JOIN `match` m ON m.id = mp.match_id
             WHERE mp.player_id = ?
               AND m.result IS NOT NULL
             GROUP BY m.result
            """;

    /** Which character the player actually favours, most-played first. */
    private static final String CHARACTER_USAGE = """
            SELECT mp.`character` AS character_type,
                   COUNT(*)       AS times_played
              FROM match_participant mp
             WHERE mp.player_id = ?
             GROUP BY mp.`character`
             ORDER BY times_played DESC
            """;

    /**
     * Best clear time per level. {@code MIN} because lower is better on every
     * board in this game — see Backend Schema §7.
     */
    private static final String BEST_LEVEL_TIMES = """
            SELECT lr.level_number          AS level_number,
                   MIN(lr.clear_time_sec)   AS best_time_sec
              FROM level_result lr
              JOIN match_participant mp ON mp.match_id = lr.match_id
             WHERE mp.player_id = ?
               AND lr.cleared = TRUE
             GROUP BY lr.level_number
             ORDER BY lr.level_number
            """;

    /** The player's rank on one board: how many players beat them, plus one. */
    private static final String RANK_ON_BOARD = """
            SELECT COUNT(*) + 1
              FROM leaderboard_entry le
             WHERE le.board = ?
               AND le.value < (SELECT le2.value
                                 FROM leaderboard_entry le2
                                WHERE le2.board = ?
                                  AND le2.player_id = ?)
            """;

    public Map<String, Object> careerTotals(UUID playerId) {
        return jdbc.queryForMap(CAREER_TOTALS, SaveSlotDao.toBytes(playerId));
    }

    /** e.g. {@code {VICTORY=7, DEFEAT=2, ABORTED=1}}. */
    public List<Map<String, Object>> matchOutcomes(UUID playerId) {
        return jdbc.queryForList(MATCH_OUTCOMES, SaveSlotDao.toBytes(playerId));
    }

    public List<Map<String, Object>> characterUsage(UUID playerId) {
        return jdbc.queryForList(CHARACTER_USAGE, SaveSlotDao.toBytes(playerId));
    }

    public List<Map<String, Object>> bestLevelTimes(UUID playerId) {
        return jdbc.queryForList(BEST_LEVEL_TIMES, SaveSlotDao.toBytes(playerId));
    }

    /** @return 1-based rank, or 0 when the player has no entry on that board. */
    public int rankOnBoard(String board, UUID playerId) {
        byte[] id = SaveSlotDao.toBytes(playerId);
        try {
            Integer rank = jdbc.queryForObject(RANK_ON_BOARD, Integer.class, board, board, id);
            return rank == null ? 0 : rank;
        } catch (org.springframework.dao.EmptyResultDataAccessException noEntry) {
            return 0;
        }
    }
}
