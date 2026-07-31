// Root build file — shared toolchain config only. Each module declares its own deps.
allprojects {
    group = "com.infectedhour"
    version = "2.0.0-sprint"
    repositories {
        mavenCentral()
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") } // libGDX snapshots if needed
    }
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }
}

// NOTE: junit-platform-launcher is declared per-module, not here. Adding it from
// a root `subprojects { dependencies { ... } }` block mutates testRuntimeOnly
// (and therefore runtimeOnly) after Spring Boot's plugin has already resolved
// :backend:runtimeClasspath, which fails bootJar with "Cannot mutate the
// dependency attributes of configuration ':backend:runtimeOnly'".
