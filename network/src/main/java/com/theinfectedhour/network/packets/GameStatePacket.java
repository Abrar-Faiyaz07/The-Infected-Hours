package com.theinfectedhour.network.packets;

import java.util.Map;

/** Host -> client on change (not every tick): contamination %, mission progress, boss phase. */
public class GameStatePacket extends Packet {

    private float contaminationPercent;
    private Map<String, Float> missionProgress;
    private String bossPhase;

    @Override
    public byte[] serialize() {
        // TODO: implement — blocked on wire-format decision (Kryo vs manual, Architecture.md §7)
        return new byte[0];
    }

    @Override
    public void deserialize(byte[] data) {
        // TODO: implement
    }
}
