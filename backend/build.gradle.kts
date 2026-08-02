// backend: Spring Boot 3.x, Spring Data JPA, Spring Security (JWT), MySQL
// Spring Boot 3.5.x, not 3.3.0: the 3.3 Gradle plugin mutates runtimeOnly while
// building bootJar, which Gradle 9 rejects with "Cannot mutate the dependency
// attributes of configuration ':backend:runtimeOnly' after ... runtimeClasspath
// was resolved". The wrapper here is Gradle 9.6.1, so the plugin has to be a
// version that supports it.
plugins {
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    implementation(project(":shared"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("com.mysql:mysql-connector-j")

    // JWT (HS256) per Backend Schema §6
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    // H2 kept for tests only so integration tests don't need a running MySQL
    testRuntimeOnly("com.h2database:h2:2.2.224")
    // Required from Gradle 8+: without it the test task fails to boot with
    // "Failed to load JUnit Platform".
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
