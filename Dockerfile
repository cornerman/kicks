# syntax = docker/dockerfile:1

FROM amazoncorretto:21-alpine

ENV LITESTREAM_VERSION="0.3.5"
ENV LITESTREAM_PLATFORM="linux-amd64-static"
ENV LITESTREAM_DOWNLOAD_URL="https://github.com/benbjohnson/litestream/releases/download/v${LITESTREAM_VERSION}/litestream-v${LITESTREAM_VERSION}-${LITESTREAM_PLATFORM}.tar.gz"
ARG LITEFS_CONFIG=configs/litefs.yml
ARG LITESTREAM_CONFIG=configs/litestream.yml
ARG REQUIRE_JAR=true

ENV FLYWAY_VERSION 10.6.0
ENV FLYWAY_HOME /flyway

LABEL fly_launch_runtime="Java"

# Install packages needed to build node modules
RUN apk add python3 pkgconfig build-base ca-certificates fuse3 sqlite bash

# Get litefs binary
# https://fly.io/docs/litefs/speedrun/
COPY --from=flyio/litefs:0.5 /usr/local/bin/litefs /usr/local/bin/litefs
RUN litefs version

# Get litestream library
# Download and install Litestream
RUN wget ${LITESTREAM_DOWNLOAD_URL} -O - | tar -xz
RUN mv litestream /usr/local/bin/
RUN chmod +x /usr/local/bin/litestream
RUN litestream version

WORKDIR /app

COPY $LITEFS_CONFIG litefs.yml
COPY $LITESTREAM_CONFIG litestream.yml

COPY projects/httpServe[r]/target/scala-2.13/httpServer-assembly-0.1.0-SNAPSHOT.jar httpServer.jar
RUN [ "$REQUIRE_JAR" = "false" ] || [ -f httpServer.jar ]

# Actual entrypoint/command inside litefs.yml
ENTRYPOINT litefs mount
