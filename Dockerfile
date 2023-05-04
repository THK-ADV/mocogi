FROM hseeberger/scala-sbt:11.0.14.1_1.6.2_2.13.8

LABEL maintainer="alexander.dobrynin@th-koeln.de"

WORKDIR /mocogi
COPY . .
RUN apt-get update && apt-get install -y \
  texlive-xetex \
  vim
RUN wget https://github.com/jgm/pandoc/releases/download/3.1.2/pandoc-3.1.2-1-amd64.deb  \
  && dpkg -i pandoc-3.1.2-1-amd64.deb  \
  && rm pandoc-3.1.2-1-amd64.deb
RUN sbt clean stage
RUN mkdir -p output
RUN mkdir -p output/de
RUN mkdir -p output/en
CMD target/universal/stage/bin/mocogi -Dconfig.file=conf/application-prod.conf