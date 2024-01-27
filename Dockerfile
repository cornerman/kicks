# syntax = docker/dockerfile:1

FROM amazoncorretto:21-alpine

ENV LITESTREAM_VERSION="0.3.5"
ARG LITEFS_CONFIG=configs/litefs.yml
ARG LITESTREAM_CONFIG=configs/litestream.yml

ENV FLYWAY_VERSION 10.6.0
ENV FLYWAY_HOME /flyway

LABEL fly_launch_runtime="Java"

# Install packages needed to build node modules
RUN apk add python3 pkgconfig build-base ca-certificates fuse3 sqlite bash

# Get litefs binary
# https://fly.io/docs/litefs/speedrun/
COPY --from=flyio/litefs:0.5 /usr/local/bin/litefs /usr/local/bin/litefs
RUN litefs version

WORKDIR /app

COPY $LITEFS_CONFIG litefs.yml

COPY projects/httpServer/target/scala-3.*/httpServer-assembly-0.1.0-SNAPSHOT.jar httpServer.jar

# Actual entrypoint/command inside litefs.yml
ENTRYPOINT litefs mount
