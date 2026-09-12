package com.infectedhour.backend.repository;

import com.infectedhour.backend.entity.SaveSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaveSlotRepository extends JpaRepository<SaveSlot, UUID> {

    List<SaveSlot> findByPlayerIdOrderBySlotNumberAsc(UUID playerId);

    Optional<SaveSlot> findByPlayerIdAndSlotNumber(UUID playerId, int slotNumber);

    void deleteByPlayerIdAndSlotNumber(UUID playerId, int slotNumber);
}
