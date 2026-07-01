import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.2.21"
    id("org.jetbrains.intellij.platform")
}

val pluginGroup: String by project
val pluginName: String by project
val pluginId: String by project
val pluginVersion: String by project

group = pluginGroup
version = pluginVersion

kotlin {
    jvmToolchain(21)
}

fun detectLocalRider(): String? {
    val candidates = listOf(
        "C:/Program Files/JetBrains/JetBrains Rider 2025.3.3",
        "C:/Program Files/JetBrains/Rider"
    )
    return candidates.firstOrNull { file(it).resolve("product-info.json").isFile }
}

val riderIdePath = providers.gradleProperty("riderIdePath")
    .orElse(providers.environmentVariable("RIDER_IDE_PATH"))
    .orElse(provider { detectLocalRider().orEmpty() })

dependencies {
    intellijPlatform {
        val localRider = riderIdePath.get()
        if (localRider.isNotBlank() && file(localRider).exists()) {
            local(localRider)
        } else {
            create("RD", "2025.3.4.1") {
                useInstaller = false
            }
        }

        testFramework(TestFrameworkType.Platform)
    }

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        id = pluginId
        name = pluginName
        version = pluginVersion

        description = """
            Paste C#/.NET stack traces into Rider, inspect them as a tree, and click frames to open source files at the logged line.
        """.trimIndent()

        vendor {
            name = "Local Tools"
        }

        ideaVersion {
            sinceBuild = "253"
        }
    }

    pluginVerification {
        ides {
            val localRider = riderIdePath.get()
            if (localRider.isNotBlank() && file(localRider).exists()) {
                local(localRider)
            } else {
                create("RD", "2025.3.4.1") {
                    useInstaller = false
                }
            }
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
