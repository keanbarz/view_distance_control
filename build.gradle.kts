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

    maven("https://repo.papermc.io/repository/maven-public/") {
        content {
            includeGroup("io.papermc.paper")
            includeGroup("io.papermc")
        }
    }

    maven("https://repo.aikar.co/nexus/content/repositories/aikar/") {
    	content {
        	includeGroup("net.md-5")
    	}
    }

    maven("https://repo.loohpjames.com/repository") {
    	content {
        	includeGroup("com.mojang")
    	}
    }

    maven("https://repo.essentialsx.net/releases/") {
        content {
            includeGroup("net.essentialsx")
        }
    }

    maven("https://repo.essentialsx.net/snapshots/") {
        content {
            includeGroup("net.essentialsx")
        }
    }

    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.Zrips")
            includeGroup("com.github.placeholderapi")
        }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.github.Zrips:CMI-API:9.8.6.4")
    compileOnly("net.essentialsx:EssentialsX:2.21.2") {
        exclude(group = "io.papermc.paper")
        exclude(group = "org.spigotmc")
    }
    compileOnly("com.github.placeholderapi:placeholderapi:2.11.6") {
        exclude(group = "io.papermc.paper")
        exclude(group = "org.spigotmc")
    }
    compileOnly("net.md-5:bungeecord-chat:1.21-R0.2-deprecated+build.21")
    compileOnly("io.papermc:paperlib:1.0.6")

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
