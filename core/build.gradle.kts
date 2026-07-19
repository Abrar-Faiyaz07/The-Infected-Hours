// core: platform-agnostic libGDX game logic (TRD §3).
// Depends on :shared for DTOs/network messages. No Spring, no JavaFX here.
dependencies {
    implementation(project(":shared"))

    implementation("com.badlogicgames.gdx:gdx:1.12.1")

    // KryoNet (TCP + UDP host-authoritative networking, TRD §1/§5)
    implementation("com.esotericsoftware:kryonet:2.22.0-RC1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
}
