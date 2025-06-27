// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql

import features.FeatureAgentResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class NoFeatureTest {

    @Autowired
    var agentResolver: FeatureAgentResolver? = null

    @Test
    fun `test FeatureAgentResolver is not defined`() {
        assertThat(agentResolver).isNull()
    }
}
