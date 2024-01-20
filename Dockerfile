# syntax = docker/dockerfile:1

FROM amazoncorretto:21-alpine

ARG LITEFS_CONFIG=litefs.config

ENV FLYWAY_VERSION 10.6.0
ENV FLYWAY_HOME /flyway

LABEL fly_launch_runtime="Java"

# Install packages needed to build node modules
RUN apk add python3 pkgconfig build-base ca-certificates fuse3 sqlite bash

# Get litefs binary
# https://fly.io/docs/litefs/speedrun/
COPY --from=flyio/litefs:0.5 /usr/local/bin/litefs /usr/local/bin/litefs

# flyway migrations
RUN wget -qO- https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/${FLYWAY_VERSION}/flyway-commandline-${FLYWAY_VERSION}-linux-alpine-x64.tar.gz | tar xvz
RUN ln -s /flyway-${FLYWAY_VERSION}/flyway /usr/local/bin

WORKDIR /app

COPY httpServer/target/scala-2.13/httpServer-assembly-0.1.0-SNAPSHOT.jar httpServer.jar
COPY $LITEFS_CONFIG litefs.yml
COPY migrations migrations

# Actual entrypoint/command inside litefs.yml
ENTRYPOINT litefs mount
