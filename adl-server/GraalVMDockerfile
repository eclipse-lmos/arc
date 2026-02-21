# SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
#
# SPDX-License-Identifier: Apache-2.0

# Stage 1: Build the native image
FROM ghcr.io/graalvm/native-image-community:21 AS build

WORKDIR /app

# Copy the entire repository to ensure all dependent modules are available
COPY . .

# Ensure gradlew is executable
RUN chmod +x gradlew

RUN ./gradlew :adl-server:clean

# Build the native image using Gradle
# -x test skips tests
# --info provides more detailed logs to diagnose the error
# -Dorg.gradle.jvmargs sets memory limits for the Gradle daemon to prevent it from consuming all RAM before Native Image runs
RUN ./gradlew :adl-server:nativeCompile -x test --info -Dorg.gradle.jvmargs="-Xmx2g"

# Stage 2: Create the runtime image
FROM gcr.io/distroless/base-debian12

WORKDIR /app

# Copy the native binary from the build stage
COPY --from=build /app/adl-server/build/native/nativeCompile/adl-server /app/server

EXPOSE 8080

# Set the entrypoint to the native binary
ENTRYPOINT ["/app/server"]
