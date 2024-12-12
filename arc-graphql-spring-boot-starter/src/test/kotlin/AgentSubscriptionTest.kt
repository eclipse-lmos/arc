// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.lmos.arc.graphql.inbound.AgentSubscription
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AgentSubscriptionTest {

    @Autowired
    lateinit var agentSubscription: AgentSubscription

    @Test
    fun `test agentSubscription is defined`(): Unit = runBlocking {
        assertThat(agentSubscription).isNotNull
    }
}
