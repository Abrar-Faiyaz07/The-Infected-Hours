package com.infectedhour.core.net;

import com.esotericsoftware.kryonet.EndPoint;
import com.infectedhour.shared.network.NetworkMessages;

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
    }
}
