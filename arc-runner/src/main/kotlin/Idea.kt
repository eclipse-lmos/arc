// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.runner

import picocli.CommandLine.Command
import java.io.File
import java.nio.charset.StandardCharsets

@Command(
    name = "idea",
    mixinStandardHelpOptions = true,
    description = ["Creates an IntelliJ IDEA library file that should enable autocompletion of the Arc DSL in IDE."],
)
open class Idea : Runnable {

    private val ideaLibrary = """
   <component name="libraryTable">
  <library name="eclipse.lmos.arc.scripting" type="repository">
    <properties maven-id="org.eclipse.lmos:arc-scripting:0.121.0" />
    <CLASSES>
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/eclipse/lmos/arc-scripting/0.121.0/arc-scripting-0.121.0.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlin/kotlin-stdlib/2.1.10/kotlin-stdlib-2.1.10.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/annotations/13.0/annotations-13.0.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlinx/kotlinx-coroutines-slf4j/1.10.1/kotlinx-coroutines-slf4j-1.10.1.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.10.1/kotlinx-coroutines-core-jvm-1.10.1.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlinx/kotlinx-coroutines-jdk8/1.10.1/kotlinx-coroutines-jdk8-1.10.1.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlinx/kotlinx-coroutines-reactor/1.10.1/kotlinx-coroutines-reactor-1.10.1.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/io/projectreactor/reactor-core/3.4.1/reactor-core-3.4.1.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/reactivestreams/reactive-streams/1.0.3/reactive-streams-1.0.3.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlinx/kotlinx-coroutines-reactive/1.10.1/kotlinx-coroutines-reactive-1.10.1.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlinx/kotlinx-serialization-json-jvm/1.8.0/kotlinx-serialization-json-jvm-1.8.0.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlinx/kotlinx-serialization-core-jvm/1.8.0/kotlinx-serialization-core-jvm-1.8.0.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/eclipse/lmos/arc-result/0.121.0/arc-result-0.121.0.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/eclipse/lmos/arc-agents/0.121.0/arc-agents-0.121.0.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlin/kotlin-scripting-common/2.1.0/kotlin-scripting-common-2.1.0.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlin/kotlin-scripting-jvm/2.1.0/kotlin-scripting-jvm-2.1.0.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlin/kotlin-script-runtime/2.1.0/kotlin-script-runtime-2.1.0.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlin/kotlin-scripting-jvm-host/2.1.0/kotlin-scripting-jvm-host-2.1.0.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlin/kotlin-compiler-embeddable/2.1.0/kotlin-compiler-embeddable-2.1.0.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlin/kotlin-reflect/1.6.10/kotlin-reflect-1.6.10.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlin/kotlin-daemon-embeddable/2.1.0/kotlin-daemon-embeddable-2.1.0.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/intellij/deps/trove4j/1.0.20200330/trove4j-1.0.20200330.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlin/kotlin-scripting-compiler-embeddable/2.1.0/kotlin-scripting-compiler-embeddable-2.1.0.jar!/" />
      <root url="jar://${'$'}MAVEN_REPOSITORY${'$'}/org/jetbrains/kotlin/kotlin-scripting-compiler-impl-embeddable/2.1.0/kotlin-scripting-compiler-impl-embeddable-2.1.0.jar!/" />
    </CLASSES>
    <JAVADOC />
    <SOURCES />
  </library>
    """.trimIndent()

    override fun run() {
        val folder = File(".idea/libraries/").also { it.mkdirs() }
        val libraryFile = File(folder, "eclipse.lmos.arc.scripting.xml").writeText(ideaLibrary, StandardCharsets.UTF_8)
        println("Created eclipse.lmos.arc.scripting.xml")
    }
}
