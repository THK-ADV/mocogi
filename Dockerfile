FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.8.3_2.13.10 as sbt
WORKDIR /mocogi
COPY . .
RUN sbt clean stage

FROM openjdk:17.0.2-slim
ARG pandoc_platform_suffix=amd64
# ARG pandoc_platform_suffix=arm64
WORKDIR /mocogi
COPY --from=sbt /mocogi/target/universal/stage .
RUN apt-get update && apt-get install -y \
  # texlive-xetex \
  wget \
  vim
RUN wget https://github.com/jgm/pandoc/releases/download/3.1.2/pandoc-3.1.2-1-$pandoc_platform_suffix.deb  \
  && dpkg -i pandoc-3.1.2-1-$pandoc_platform_suffix.deb  \
  && rm pandoc-3.1.2-1-$pandoc_platform_suffix.deb
RUN mkdir -p logs
RUN mkdir -p output
RUN mkdir -p output/de
RUN mkdir -p output/en
CMD bin/mocogi -Dconfig.file=conf/application-prod.conf