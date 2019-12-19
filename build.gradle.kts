import org.zaproxy.gradle.addon.AddOnPlugin
import org.zaproxy.gradle.addon.AddOnStatus
import org.zaproxy.gradle.addon.misc.ConvertMarkdownToHtml
import org.zaproxy.gradle.addon.misc.CreateGitHubRelease
import org.zaproxy.gradle.addon.misc.ExtractLatestChangesFromChangelog

plugins {
    `java-library`
    eclipse
    id("org.zaproxy.add-on") version "0.2.0"
}

eclipse {
    classpath {
        // Prevent compilation of zapHomeFiles.
        sourceSets = listOf()
    }
}

repositories {
    mavenCentral()
}

version = "1.0.1"
description = "Colors history table items based on tags"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

zapAddOn {
    addOnId.set(project.name.replace("-", ""))
    addOnName.set("Neonmarker")
    addOnStatus.set(AddOnStatus.ALPHA)
    zapVersion.set("2.8.0")

    releaseLink.set("https://github.com/kingthorin/neonmarker/compare/v@PREVIOUS_VERSION@...v@CURRENT_VERSION@")
    unreleasedLink.set("https://github.com/kingthorin/neonmarker/compare/v@CURRENT_VERSION@...HEAD")

    manifest {
        author.set("Juha Kivekäs, kingthorin")
        url.set("https://github.com/kingthorin/neonmarker")
        changesFile.set(tasks.named<ConvertMarkdownToHtml>("generateManifestChanges").flatMap { it.html })

        helpSet {
            baseName.set("help%LC%.helpset")
            localeToken.set("%LC%")
        }
    }
}

System.getenv("GITHUB_REF")?.let { ref ->
    if ("refs/tags/" !in ref) {
        return@let
    }

    tasks.register<CreateGitHubRelease>("createReleaseFromGitHubRef") {
        val targetTag = ref.removePrefix("refs/tags/")
        val targetAddOnVersion = targetTag.removePrefix("v")

        authToken.set(System.getenv("GITHUB_TOKEN"))
        repo.set(System.getenv("GITHUB_REPOSITORY"))
        tag.set(targetTag)

        title.set(provider { "v${zapAddOn.addOnVersion.get()}" })
        bodyFile.set(tasks.named<ExtractLatestChangesFromChangelog>("extractLatestChanges").flatMap { it.latestChanges })

        assets {
            register("add-on") {
                file.set(tasks.named<Jar>(AddOnPlugin.JAR_ZAP_ADD_ON_TASK_NAME).flatMap { it.archiveFile })
            }
        }

        doFirst {
            val addOnVersion = zapAddOn.addOnVersion.get()
            require(addOnVersion == targetAddOnVersion) {
                "Version of the tag $targetAddOnVersion does not match the version of the add-on $addOnVersion"
            }
        }
    }
}
