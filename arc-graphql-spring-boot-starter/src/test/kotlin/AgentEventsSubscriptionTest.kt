// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.graphql.inbound.EventSubscription
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class AgentEventsSubscriptionTest {

    @Autowired
    lateinit var eventSubscription: EventSubscription

    @Test
    fun `test eventSubscription is defined`(): Unit = runBlocking {
        assertThat(eventSubscription).isNotNull
    }
}
