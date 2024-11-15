FROM sbtscala/scala-sbt:eclipse-temurin-jammy-21.0.2_13_1.10.1_3.3.3 as sbt
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
RUN mkdir -p output/de
RUN mkdir -p output/en
RUN mkdir -p output/catalogs
RUN mkdir -p output/electives
RUN mkdir -p mc
RUN mkdir -p mc/intro
CMD bin/mocogi -Dconfig.file=conf/application-prod.conf