FROM hseeberger/scala-sbt:11.0.14.1_1.6.2_2.13.8

LABEL maintainer="alexander.dobrynin@th-koeln.de"

WORKDIR /mocogi
COPY . .
RUN apt-get update && apt-get install -y \
  texlive-xetex \
  vim
RUN wget https://github.com/jgm/pandoc/releases/download/2.18/pandoc-2.18-1-amd64.deb  \
  && dpkg -i pandoc-2.18-1-amd64.deb  \
  && rm pandoc-2.18-1-amd64.deb
RUN sbt clean stage
RUN mkdir -p output
CMD target/universal/stage/bin/mocogi -Dconfig.file=conf/application-prod.conf