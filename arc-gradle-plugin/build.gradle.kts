import com.vanniktech.maven.publish.SonatypeHost
import java.lang.System.getenv
import java.net.URI

// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("ArcGradlePlugin") {
            id = "org.eclipse.lmos.arc.gradle.plugin"
            implementationClass = "org.eclipse.lmos.arc.gradle.plugin.ArcPlugin"
            version = version
        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    pom {
        name = "ARC"
        description = "ARC is an AI framework."
        url = "https://github.com/eclipse-lmos/arc"
        licenses {
            license {
                name = "Apache-2.0"
                distribution = "repo"
                url = "https://github.com/eclipse-lmos/arc/blob/main/LICENSES/Apache-2.0.txt"
            }
        }
        developers {
            developer {
                id = "pat"
                name = "Patrick Whelan"
                email = "opensource@telekom.de"
            }
            developer {
                id = "bharat_bhushan"
                name = "Bharat Bhushan"
                email = "opensource@telekom.de"
            }
            developer {
                id = "merrenfx"
                name = "Max Erren"
                email = "opensource@telekom.de"
            }
        }
        scm {
            url = "https://github.com/eclipse-lmos/arc.git"
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/eclipse-lmos/arc")
            credentials {
                username = findProperty("GITHUB_USER")?.toString() ?: getenv("GITHUB_USER")
                password = findProperty("GITHUB_TOKEN")?.toString() ?: getenv("GITHUB_TOKEN")
            }
        }
    }
}
