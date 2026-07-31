package com.infectedhour.shared.network;

import java.util.ArrayList;
import java.util.List;

/**
 * Single source of truth for which classes must be registered with Kryo,
 * in a fixed order, on BOTH host and client (KryoNet requires identical
 * registration order on both ends). core/net/NetworkRegistration calls this.
 *
 * <p><b>Field types count, not just message types.</b> Kryo will not
 * serialize a class it has never been told about, even when that class only
 * appears as the runtime type of a field. {@link WorldSnapshot} declares
 * {@code List<PlayerState>} but the concrete object on the wire is an
 * {@link ArrayList}, and {@code CloudFrontierDelta.addedTileIndices} is an
 * {@code int[]} — both must be registered or the first snapshot broadcast
 * dies with {@code IllegalArgumentException: Class is not registered}.
 *
 * <p>Consequence for callers: build snapshot lists with {@code new ArrayList<>()}.
 * {@code List.of(...)} returns {@code ImmutableCollections.ListN}, a different
 * class that is NOT registered here and would fail the same way.
 */
public final class NetworkMessages {

    private NetworkMessages() {
    }

    public static List<Class<?>> registrationOrder() {
        return List.of(
                // --- container / field types (see class javadoc) ---
                ArrayList.class,
                int[].class,

                // --- handshake ---
                JoinRequest.class,
                JoinAccept.class,
                JoinReject.class,

                // --- per-tick traffic ---
                InputCommand.class,
                WorldSnapshot.class,
                WorldSnapshot.PlayerState.class,
                WorldSnapshot.EnemyState.class,
                WorldSnapshot.CloudFrontierDelta.class,
                WorldSnapshot.ObjectiveState.class,

                // --- reliable events ---
                EventMessage.class,
                LevelTransition.class,
                MatchResultMessage.class,

                // --- enums ---
                CharacterType.class,
                MatchMode.class
        );
    }
}
