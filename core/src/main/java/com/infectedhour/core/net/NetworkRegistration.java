package com.infectedhour.core.net;

import com.esotericsoftware.kryonet.EndPoint;
import com.infectedhour.shared.network.NetworkMessages;
import com.infectedhour.shared.network.WorldSnapshot;

/**
 * Registers every message class from shared/network with a KryoNet
 * EndPoint's Kryo instance, in the exact order NetworkMessages defines —
 * KryoNet requires identical registration order on host and client.
 * Call this on BOTH GameServer and GameClient before connecting.
 */
public final class NetworkRegistration {

    private NetworkRegistration() {
    }

    public static void register(EndPoint endPoint) {
        var kryo = endPoint.getKryo();
        for (Class<?> clazz : NetworkMessages.registrationOrder()) {
            kryo.register(clazz);
        }

        // ── NEW: Register ItemState so KryoNet can serialize ground items ──
        kryo.register(WorldSnapshot.ItemState.class);
    }
}
