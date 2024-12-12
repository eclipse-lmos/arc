// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.memory.mongo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName.parse

@SpringBootApplication(proxyBeanMethods = false)
open class TestApplication {

    @Bean
    @ServiceConnection
    fun mongoContainer() =
        MongoDBContainer(parse("mongo:7")).withReuse(true)
}
