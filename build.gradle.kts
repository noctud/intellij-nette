import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun cfg(key: String) = providers.gradleProperty(key)
fun env(key: String) = providers.environmentVariable(key)

plugins {
    kotlin("jvm") version "2.2.10"
    id("org.jetbrains.intellij.platform") version "2.9.0"
    id("org.jetbrains.changelog") version "2.4.0"
}

group = cfg("pluginGroup").get()
version = cfg("pluginVersion").get()

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    intellijPlatform {
        cfg("intellij.localPath").orNull?.let { local(it) } ?: create(cfg("platformType"), cfg("platformVersion"))
        bundledPlugin("com.jetbrains.php")
        bundledPlugin("com.intellij.modules.json")
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        name = cfg("pluginName")
        version = cfg("pluginVersion")

        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        changeNotes = provider {
            changelog.getAll().values.joinToString("\n") { changelog.renderItem(it, Changelog.OutputType.HTML) }
        }

        ideaVersion {
            sinceBuild = cfg("pluginSinceBuild")
        }
    }

    autoReload = true
}

changelog {
    version = cfg("pluginVersion")
    repositoryUrl = cfg("pluginRepositoryUrl")
    path = file("CHANGELOG.md").canonicalPath
}

tasks {
    wrapper {
        gradleVersion = cfg("gradleVersion").get()
    }

    publishPlugin {
        token = env("PLUGIN_PUBLISH_TOKEN")
    }
}
