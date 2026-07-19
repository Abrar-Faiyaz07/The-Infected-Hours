package com.infectedhour.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;

/**
 * Two uses (TRD §3 comment on this module, §11 build target):
 *  1. fx-launcher calls {@link #boot} from GameLauncherBridge on a
 *     dedicated (non-FX) thread when a match starts.
 *  2. `main` here is the dev shortcut: `./gradlew lwjgl3:run` launches
 *     host-solo without going through JavaFX at all.
 */
public class Lwjgl3Launcher {

    public static void main(String[] args) {
        // Dev shortcut: always host, no client address, a no-op bridge since there's no FX window to hand back to.
        boot(true, null, new GameBridge());
    }

    public static void boot(boolean isHost, String hostAddressIfClient, GameBridge bridge) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("The Infected Hour");
        config.setWindowedMode(1280, 720); // base virtual resolution, UI/UX doc §1
        config.useVsync(true);

        new Lwjgl3Application(new InfectedHourGame(isHost, hostAddressIfClient, bridge), config);
    }
}
