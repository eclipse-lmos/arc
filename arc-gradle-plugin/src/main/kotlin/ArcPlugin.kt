// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSetContainer

class ArcPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val build = project.layout.buildDirectory.dir("arc/kotlin").get()
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        sourceSets.getByName("main") {
            it.java.srcDir(build)
            it.compileClasspath += project.layout.buildDirectory.dir("arc/kotlin").get().files()
        }

        /**
         * Add a dependency on the `generateAgentCode` task to the `compileKotlin` task.
         */
        project.tasks.getByName("compileKotlin") {
            it.dependsOn("generateAgentCode")
        }

        /**
         * Register the task to generate the agent code.
         */
        project.registerTask<GenerateAgentCodeTask>("generateAgentCode") {
            it.group = "arc"
            it.input = project.layout.projectDirectory.dir("agents")
            it.output = build
        }
    }
}

inline fun <reified type : Task> Project.registerTask(name: String, action: Action<in type>? = null) =
    tasks.register(name, type::class.java).apply { action?.let { configure(it) } }
