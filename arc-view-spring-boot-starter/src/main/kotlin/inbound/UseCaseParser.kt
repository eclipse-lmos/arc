// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.view.inbound

import org.eclipse.lmos.arc.assistants.support.usecases.validation.UseCaseToJson
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Controller to receive use cases as plain markdown text and return them as JSON.
 */
@Controller
@RequestMapping("/arc/usecases")
class UseCaseParser {

    private val json = UseCaseToJson()

    @PostMapping(consumes = ["text/plain"], produces = ["application/json"])
    fun receiveUseCases(@RequestBody data: String): ResponseEntity<String> {
        val parsed = json.convert(data)
        return ResponseEntity.ok(parsed)
    }
}
