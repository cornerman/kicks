VERSION 0.8

base:
    FROM amazoncorretto:21-alpine
    apt-get 

build:
    FROM amazoncorretto:21-alpine
    COPY main.go .
    RUN go build -o output/example main.go
    SAVE ARTIFACT output/example AS LOCAL local-output/go-example

docker:
    FROM amazoncorretto:21-alpine
    COPY +build/example .
    ENTRYPOINT ["/go-workdir/example"]
    SAVE IMAGE go-example:latest

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
COPY modules/httpServer/target/scala-3.*/httpServer-assembly-0.1.0-SNAPSHOT.jar httpServer.jar
COPY modules/webapp/dist $FRONTEND_DISTRIBUTION_PATH

# Actual entrypoint/command inside litefs.yml
ENTRYPOINT litefs mount
