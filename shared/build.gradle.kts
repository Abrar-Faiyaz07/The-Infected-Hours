// shared: DTOs, network message classes, constants only.
// Deliberately has NO libGDX and NO Spring dependency (TRD §3) so both
// core (game) and backend can depend on it without pulling in the other's stack.
// Kryo (used by core/net via KryoNet) requires no-arg constructors on
// registered classes — kept plain for that reason, not just style.
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    // Required from Gradle 8+: without it the test task fails to boot with
    // "Failed to load JUnit Platform".
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
