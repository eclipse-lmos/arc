// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.assistants.support.usecases.features

import com.github.mustachejava.DefaultMustacheFactory
import org.eclipse.lmos.arc.assistants.support.usecases.UseCase
import java.io.StringReader
import java.io.StringWriter

/**
 * Creates a formatter that applies Mustache variables to the use case string.
 *
 * @param variables The variables to be applied to the Mustache template.
 * @return A suspend function that formats the use case string using the provided variables.
 */
fun mustache(variables: Map<String, Any>): suspend (String, UseCase, List<UseCase>?, List<String>) -> String =
    { template, _, _, _ ->
        val mf = DefaultMustacheFactory()
        val mustache = mf.compile(StringReader(template), "template")
        val writer = StringWriter()
        mustache.execute(writer, variables).close()
        writer.toString()
    }

