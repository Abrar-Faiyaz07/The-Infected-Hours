// core: platform-agnostic libGDX game logic (TRD §3).
// Depends on :shared for DTOs/network messages. No Spring, no JavaFX here.

// Put the repo-root assets/ folder on core's runtime classpath.
// Gdx.files.internal("map.png") resolves against the classpath, so declaring it
// here makes art load identically in BOTH run modes — `:lwjgl3:run` (working
// directory = lwjgl3/) and `:fx-launcher:run` (working directory = fx-launcher/).
// Relying on the working directory instead only ever works for one of them.
sourceSets["main"].resources.srcDir(rootProject.file("assets"))

dependencies {
    implementation(project(":shared"))

    implementation("com.badlogicgames.gdx:gdx:1.12.1")

    // KryoNet (TCP + UDP host-authoritative networking, TRD §1/§5)
    implementation("com.esotericsoftware:kryonet:2.22.0-RC1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    // Required from Gradle 8+: without it the test task fails to boot with
    // "Failed to load JUnit Platform".
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
