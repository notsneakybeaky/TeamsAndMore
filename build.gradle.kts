group = "io.github.itshaihtamn.teamsandmore"
version = "1.0.6"

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.1"
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven { url = uri("https://jitpack.io") }
    maven {
        name = "discordsrv"
        url = uri("https://nexus.scarsz.me/content/groups/public/")
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.107.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testImplementation("com.discordsrv:discordsrv:1.28.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("dev.triumphteam:triumph-gui:3.1.13")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.discordsrv:discordsrv:1.28.0")
    compileOnly("net.luckperms:api:5.5")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.test {
    useJUnitPlatform()
}