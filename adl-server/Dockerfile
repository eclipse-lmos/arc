# SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
#
# SPDX-License-Identifier: Apache-2.0

FROM gradle:8-jdk24-corretto AS build
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
# Ensure gradlew is executable
RUN chmod +x gradlew
# Skip tests to speed up the build
RUN ./gradlew :adl-server:installDist --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /home/gradle/project/adl-server/build/install/adl-server/ /app/
EXPOSE 8080
ENTRYPOINT ["/app/bin/adl-server"]
