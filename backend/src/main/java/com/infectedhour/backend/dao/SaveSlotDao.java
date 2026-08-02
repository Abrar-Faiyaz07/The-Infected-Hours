package com.infectedhour.backend.dao;

import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.dto.SaveSlotDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Direct-SQL data access for the nine save slots.
 *
 * <p><b>Why this exists alongside {@code SaveSlotRepository}.</b> The Spring
 * Data repository generates its SQL at runtime from method names, so the actual
 * queries are invisible in the source. This DAO writes them out explicitly.
 * That matters here for two reasons: the queries are inspectable and reviewable
 * as SQL, and a few of them — the upsert, the aggregate summary — express
 * things JPA either cannot do in one statement or does inefficiently by loading
 * whole entities first.
 *
 * <p>Both layers are kept because they serve different jobs: the JPA repository
 * handles entity lifecycle inside a transaction, this DAO handles set-based
 * reads and single-statement writes. Nothing should call both for the same
 * operation.
 *
 * <h2>UUIDs are BINARY(16)</h2>
 * Hibernate stores {@code UUID} as {@code binary(16)}, not as a 36-character
 * string. Passing a {@code UUID} straight into a JDBC parameter would bind it
 * as text and silently match nothing, so every query converts explicitly via
 * {@link #toBytes(UUID)}.
 */
@Repository
public class SaveSlotDao {

    private final JdbcTemplate jdbc;

    public SaveSlotDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------
    // SQL
    // ------------------------------------------------------------------

    private static final String COLUMNS = """
            slot_number, level_number, level_name, checkpoint_id, checkpoint_name,
            story_progress, playtime_sec, character_type, player_hp,
            personal_contamination_pct, global_contamination_pct,
            inventory_json, objectives_json, saved_at
            """;

    private static final String SELECT_ALL_FOR_PLAYER = """
            SELECT %s
              FROM save_slot
             WHERE player_id = ?
             ORDER BY slot_number ASC
            """.formatted(COLUMNS);

    private static final String SELECT_ONE = """
            SELECT %s
              FROM save_slot
             WHERE player_id = ? AND slot_number = ?
            """.formatted(COLUMNS);

    /**
     * Single-statement upsert. MySQL's {@code ON DUPLICATE KEY UPDATE} fires
     * against the unique index on (player_id, slot_number), so overwriting slot
     * 4 is one round trip with no read-modify-write race between two clients.
     */
    private static final String UPSERT = """
            INSERT INTO save_slot (
                id, player_id, slot_number, level_number, level_name,
                checkpoint_id, checkpoint_name, story_progress, playtime_sec,
                character_type, player_hp, personal_contamination_pct,
                global_contamination_pct, inventory_json, objectives_json, saved_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                level_number               = VALUES(level_number),
                level_name                 = VALUES(level_name),
                checkpoint_id              = VALUES(checkpoint_id),
                checkpoint_name            = VALUES(checkpoint_name),
                story_progress             = VALUES(story_progress),
                playtime_sec               = VALUES(playtime_sec),
                character_type             = VALUES(character_type),
                player_hp                  = VALUES(player_hp),
                personal_contamination_pct = VALUES(personal_contamination_pct),
                global_contamination_pct   = VALUES(global_contamination_pct),
                inventory_json             = VALUES(inventory_json),
                objectives_json            = VALUES(objectives_json),
                saved_at                   = VALUES(saved_at)
            """;

    private static final String DELETE_ONE =
            "DELETE FROM save_slot WHERE player_id = ? AND slot_number = ?";

    private static final String COUNT_USED =
            "SELECT COUNT(*) FROM save_slot WHERE player_id = ?";

    private static final String SELECT_MOST_RECENT = """
            SELECT %s
              FROM save_slot
             WHERE player_id = ?
             ORDER BY saved_at DESC
             LIMIT 1
            """.formatted(COLUMNS);

    /** Furthest level reached across all nine slots — one aggregate instead of nine reads. */
    private static final String SELECT_FURTHEST_LEVEL =
            "SELECT COALESCE(MAX(level_number), 0) FROM save_slot WHERE player_id = ?";

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    public List<SaveSlotDto> findAllByPlayer(UUID playerId) {
        return jdbc.query(SELECT_ALL_FOR_PLAYER, ROW_MAPPER, toBytes(playerId));
    }

    public SaveSlotDto findSlot(UUID playerId, int slotNumber) {
        List<SaveSlotDto> found = jdbc.query(SELECT_ONE, ROW_MAPPER, toBytes(playerId), slotNumber);
        return found.isEmpty() ? SaveSlotDto.empty(slotNumber) : found.get(0);
    }

    /** "Continue" on the main menu — the slot the player touched last. */
    public SaveSlotDto findMostRecent(UUID playerId) {
        List<SaveSlotDto> found = jdbc.query(SELECT_MOST_RECENT, ROW_MAPPER, toBytes(playerId));
        return found.isEmpty() ? null : found.get(0);
    }

    public int countUsedSlots(UUID playerId) {
        Integer count = jdbc.queryForObject(COUNT_USED, Integer.class, toBytes(playerId));
        return count == null ? 0 : count;
    }

    public int findFurthestLevel(UUID playerId) {
        Integer level = jdbc.queryForObject(SELECT_FURTHEST_LEVEL, Integer.class, toBytes(playerId));
        return level == null ? 0 : level;
    }

    /** Insert or overwrite in one statement. @return rows affected (1 = insert, 2 = update in MySQL). */
    public int upsert(UUID playerId, int slotNumber, SaveSlotDto slot) {
        if (!GameConstants.isValidSaveSlot(slotNumber)) {
            throw new IllegalArgumentException(
                    "Slot must be 1.." + GameConstants.SAVE_SLOT_COUNT + ", got " + slotNumber);
        }
        return jdbc.update(UPSERT,
                toBytes(UUID.randomUUID()),   // only used when the INSERT branch wins
                toBytes(playerId),
                slotNumber,
                slot.levelNumber(),
                slot.levelName(),
                slot.checkpointId(),
                slot.checkpointName(),
                slot.storyProgress(),
                slot.playtimeSec(),
                slot.characterType(),
                slot.playerHp(),
                slot.personalContaminationPct(),
                slot.globalContaminationPct(),
                slot.inventoryJson(),
                slot.objectivesJson(),
                Timestamp.from(Instant.now()));
    }

    public int delete(UUID playerId, int slotNumber) {
        return jdbc.update(DELETE_ONE, toBytes(playerId), slotNumber);
    }

    // ------------------------------------------------------------------
    // Mapping
    // ------------------------------------------------------------------

    private static final RowMapper<SaveSlotDto> ROW_MAPPER = SaveSlotDao::mapRow;

    private static SaveSlotDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp savedAt = rs.getTimestamp("saved_at");
        return new SaveSlotDto(
                rs.getInt("slot_number"),
                true, // a row exists, so the slot is occupied by definition
                rs.getInt("level_number"),
                rs.getString("level_name"),
                rs.getString("checkpoint_id"),
                rs.getString("checkpoint_name"),
                rs.getInt("story_progress"),
                rs.getLong("playtime_sec"),
                rs.getString("character_type"),
                rs.getFloat("player_hp"),
                rs.getFloat("personal_contamination_pct"),
                rs.getFloat("global_contamination_pct"),
                rs.getString("inventory_json"),
                rs.getString("objectives_json"),
                savedAt == null ? null : savedAt.toInstant().toString());
    }

    /** Hibernate persists UUID as BINARY(16); JDBC parameters must match that form. */
    static byte[] toBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }
}
