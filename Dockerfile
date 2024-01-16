# syntax = docker/dockerfile:1

FROM amazoncorretto:21-alpine

LABEL fly_launch_runtime="Java"

# Install packages needed to build node modules
RUN apk add python3 pkgconfig build-base ca-certificates fuse3 sqlite

# Get litefs binary
# https://fly.io/docs/litefs/speedrun/
COPY --from=flyio/litefs:0.5 /usr/local/bin/litefs /usr/local/bin/litefs

WORKDIR /app

COPY httpServer/target/scala-2.13/httpServer-assembly-0.1.0-SNAPSHOT.jar httpServer.jar

# Actual entrypoint/command inside litefs.yml
ENTRYPOINT litefs mount
