package com.infectedhour.shared.network;

import java.util.List;

/**
 * Single source of truth for which classes must be registered with Kryo,
 * in a fixed order, on BOTH host and client (KryoNet requires identical
 * registration order on both ends). core/net/NetworkRegistration calls this.
 */
public final class NetworkMessages {

    private NetworkMessages() {
    }

    public static List<Class<?>> registrationOrder() {
        return List.of(
                JoinRequest.class,
                JoinAccept.class,
                JoinReject.class,
                InputCommand.class,
                WorldSnapshot.class,
                WorldSnapshot.PlayerState.class,
                WorldSnapshot.EnemyState.class,
                WorldSnapshot.CloudFrontierDelta.class,
                WorldSnapshot.ObjectiveState.class,
                EventMessage.class,
                LevelTransition.class,
                MatchResultMessage.class,
                CharacterType.class,
                MatchMode.class
        );
    }
}
