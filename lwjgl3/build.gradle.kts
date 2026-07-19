// lwjgl3: LWJGL3 backend. Used two ways (TRD §3, §11):
//  1. Booted BY fx-launcher on a dedicated thread when a match starts.
//  2. Run directly via `./gradlew lwjgl3:run` as a dev shortcut that skips
//     the JavaFX launcher entirely (host-solo, for quick iteration).
plugins {
    application
}

dependencies {
    implementation(project(":core"))
    implementation(project(":shared"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.12.1")
    implementation("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-desktop")
}

application {
    mainClass.set("com.infectedhour.lwjgl3.Lwjgl3Launcher")
}
