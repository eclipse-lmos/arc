// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.runner.server

import java.io.File

data class AppConfig(
    val clientConfig: AIClientConfig,
    val scriptFolder: File,
)
