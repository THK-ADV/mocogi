FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.8.3_2.13.10 as sbt
ARG GITHUB_TOKEN
ENV GITHUB_TOKEN=$GITHUB_TOKEN
WORKDIR /mocogi
COPY . .
RUN sbt clean stage

FROM mocogi-core:latest
ARG GIT_EMAIL
ARG GIT_USERNAME
ARG GIT_HOST
ARG GIT_ACCESS_TOKEN
ARG GIT_REPO_PATH
ARG GIT_REPO_NAME
WORKDIR /mocogi
COPY --from=sbt /mocogi/target/universal/stage .
COPY --from=sbt /mocogi/gitlab-init.sh .
COPY --from=sbt /mocogi/gitlab-push.sh .
COPY --from=sbt /mocogi/gitlab-switch-branch.sh .
COPY --from=sbt /mocogi/gitlab-diff-preview.sh .
RUN chmod +x gitlab-init.sh
RUN chmod +x gitlab-push.sh
RUN chmod +x gitlab-switch-branch.sh
RUN chmod +x gitlab-diff-preview.sh
RUN mkdir -p tmp
RUN mkdir -p logs
RUN mkdir -p output
RUN mkdir -p output/de
RUN mkdir -p output/en
RUN mkdir -p output/catalogs
RUN mkdir -p output/wpfs
RUN ./gitlab-init.sh $GIT_EMAIL $GIT_USERNAME $GIT_HOST $GIT_ACCESS_TOKEN $GIT_REPO_PATH $GIT_REPO_NAME
RUN rm gitlab-init.sh
CMD bin/mocogi -Dconfig.file=conf/application-prod.conf