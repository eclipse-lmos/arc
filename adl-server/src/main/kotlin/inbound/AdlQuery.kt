// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query

class AdlQuery : Query {

    @GraphQLDescription("Returns the supported version of the ALD.")
    fun version(adlCode: AdlSource): String {
        return "!.0.0"
    }
}
