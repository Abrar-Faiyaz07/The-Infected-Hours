// fx-launcher: JavaFX pre-game app — login, lobby, settings, profile,
// leaderboards, results (TRD §2, §3). Boots :lwjgl3 on a dedicated thread
// when a match starts; NEVER touches Gdx classes from the FX thread.
plugins {
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

javafx {
    version = "21.0.2"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":core"))
    implementation(project(":lwjgl3"))

    // HTTP client for REST calls to :backend — Java 11+ HttpClient (TRD §1) + Jackson for (de)serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
}

application {
    mainClass.set("com.infectedhour.fxlauncher.LauncherApplication")
}

// The launcher boots libGDX in-process, so it needs the same asset working
// directory as :lwjgl3 — see that module's build file.
tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("assets")
}
