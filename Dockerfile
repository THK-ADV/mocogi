FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.8.3_2.13.10 as sbt
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
CMD bin/mocogi -Dconfig.file=conf/application-prod.conf