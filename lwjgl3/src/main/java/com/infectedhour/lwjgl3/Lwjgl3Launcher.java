package com.infectedhour.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.SessionConfig;

/**
 * Two uses (TRD §3 comment on this module, §11 build target):
 *  1. fx-launcher calls {@link #boot} from GameLauncherBridge on a
 *     dedicated (non-FX) thread when a match starts.
 *  2. {@code main} here is the dev shortcut: {@code ./gradlew lwjgl3:run}
 *     launches host-solo without going through JavaFX at all.
 */
public class Lwjgl3Launcher {

    /**
     * Dev shortcut with a no-op bridge, since there is no FX window to hand back to.
     *
     * <pre>
     *   ./gradlew :lwjgl3:run                             # host, solo
     *   ./gradlew :lwjgl3:run --args="host"               # host, waits for a partner
     *   ./gradlew :lwjgl3:run --args="join 192.168.0.14"  # join that host
     *   ./gradlew :lwjgl3:run --args="join"               # join localhost (two windows, one PC)
     * </pre>
     *
     * The two-windows-on-one-PC form is the fastest way to exercise the whole
     * networking path without a second laptop.
     */
    public static void main(String[] args) {
        boot(parseSession(args), new GameBridge());
    }

    static SessionConfig parseSession(String[] args) {
        if (args != null && args.length > 0 && "join".equalsIgnoreCase(args[0])) {
            String host = args.length > 1 ? args[1] : "localhost";
            return SessionConfig.joining(host, "dev-client", "Dev Jane");
        }
        if (args != null && args.length > 0 && "jane".equalsIgnoreCase(args[0])) {
            return SessionConfig.hosting("dev-host", "Dev Host", null, com.infectedhour.shared.network.CharacterType.JANE);
        }
        return SessionConfig.devSolo();
    }

    public static void boot(SessionConfig session, GameBridge bridge) {
        boot(session, bridge, true);
    }

    /**
     * Starts the native game window. Fullscreen is the default because the
     * 1280x720 value in the UI specification is a virtual rendering size, not
     * the intended physical desktop-window size.
     */
    public static void boot(SessionConfig session, GameBridge bridge, boolean fullscreen) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("The Infected Hour");
        config.setWindowedMode(1280, 720);
        config.setMaximized(true);
        config.setResizable(true);
        config.useVsync(true);

        new Lwjgl3Application(new InfectedHourGame(session, bridge), config);
    }
}
