plugins {
    java
}

group = "org.rainbowhunter"
version = (project.findProperty("pluginVersion") as String?) ?: "dev"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://repo.essentialsx.net/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.github.Zrips:CMI-API:9.8.6.4")
    compileOnly("net.essentialsx:EssentialsX:2.21.2") {
        exclude(group = "io.papermc.paper")
        exclude(group = "org.spigotmc")
    }
    compileOnly("me.clip:placeholderapi:2.11.6") {
        exclude(group = "io.papermc.paper")
        exclude(group = "org.spigotmc")
    }

    testImplementation("io.papermc.paper:paper-api:26.1.2.build.+")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveFileName.set("ViewDistanceControl-${version}.jar")
}
