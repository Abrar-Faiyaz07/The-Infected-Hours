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

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
