# syntax = docker/dockerfile:1

FROM amazoncorretto:21-alpine

ARG LITEFS_CONFIG=configs/litefs.yml
ARG REQUIRE_JAR=true

ENV FLYWAY_VERSION 10.6.0
ENV FLYWAY_HOME /flyway

LABEL fly_launch_runtime="Java"

# Install packages needed to build node modules
RUN apk add python3 pkgconfig build-base ca-certificates fuse3 sqlite bash

# Get litefs binary
# https://fly.io/docs/litefs/speedrun/
COPY --from=flyio/litefs:0.5 /usr/local/bin/litefs /usr/local/bin/litefs

WORKDIR /app

COPY $LITEFS_CONFIG litefs.yml

COPY httpServe[r]/target/scala-2.13/httpServer-assembly-0.1.0-SNAPSHOT.jar httpServer.jar
RUN [ "$REQUIRE_JAR" = "false" ] || [ -f httpServer.jar ]

# Actual entrypoint/command inside litefs.yml
ENTRYPOINT litefs mount
