// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21

//COMPILE_OPTIONS -Xlint:-options

//DEPS org.eclipse.lmos:arc-runner:0.107.0
//DEPS org.slf4j:slf4j-api:2.0.16
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.2
//DEPS io.projectreactor:reactor-core:3.6.9

package arc.runner;

import org.eclipse.lmos.arc.runner.Arc;
import picocli.CommandLine;

/* ktlint-disable */
public class arc {

    public static void main(String[] args) {
        var exitCode = new CommandLine(new Arc()).execute(args);
        // System.exit(exitCode); kills the server
    }
}
/* ktlint-enable */
