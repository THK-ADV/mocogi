FROM sbtscala/scala-sbt:eclipse-temurin-21.0.13_9_1.13.1_3.9.0 AS sbt
ARG GITHUB_TOKEN
ENV GITHUB_TOKEN=$GITHUB_TOKEN
WORKDIR /mocogi
COPY . .
RUN sbt clean stage
FROM mocogi-core:latest
WORKDIR /mocogi
COPY --from=sbt /mocogi/target/universal/stage .
RUN mkdir -p tmp
RUN mkdir -p logs
RUN mkdir -p output
RUN mkdir -p output/catalogs
RUN mkdir -p output/examlist
RUN mkdir -p mc
RUN mkdir -p mc/intro
RUN mkdir -p mc/assets
RUN git clone $GIT_REPO_URL
CMD bin/mocogi -Dconfig.file=conf/application-prod.conf
