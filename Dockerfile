# syntax = docker/dockerfile:1

FROM amazoncorretto:21-alpine

ARG LITEFS_CONFIG=configs/litefs.yml
ENV FRONTEND_DISTRIBUTION_PATH=./dist

LABEL fly_launch_runtime="Java"

# Install packages needed to build node modules
RUN apk add python3 pkgconfig build-base ca-certificates fuse3 sqlite bash

# Get litefs binary
# https://fly.io/docs/litefs/speedrun/
COPY --from=flyio/litefs:0.5 /usr/local/bin/litefs /usr/local/bin/litefs
RUN litefs version

WORKDIR /app

COPY $LITEFS_CONFIG litefs.yml
COPY modules/httpServer/target/httpServer.jar
COPY modules/webapp/dist $FRONTEND_DISTRIBUTION_PATH

# Actual entrypoint/command inside litefs.yml
ENTRYPOINT litefs mount
