ARG SONARQUBE_VERSION="community"
ARG WORKDIR="/home/build/project"

FROM gradle:8.9-jdk21-jammy AS builder
ARG WORKDIR

COPY . ${WORKDIR}
WORKDIR ${WORKDIR}
RUN gradle build -x test


FROM node:22.16-alpine AS webapp-builder
ARG WORKDIR

COPY ./sonarqube-webapp ${WORKDIR}
COPY ./sonarqube-webapp-addons ${WORKDIR}/libs/sq-server-addons

WORKDIR ${WORKDIR}
RUN yarn install
RUN yarn nx run sq-server:build


FROM sonarqube:${SONARQUBE_VERSION}
ARG PLUGIN_VERSION
ARG WORKDIR

COPY --from=builder --chown=sonarqube:root ${WORKDIR}/build/libs/sonarqube-community-branch-plugin-*.jar /opt/sonarqube/extensions/plugins/

RUN chmod -R 770 /opt/sonarqube/web && rm -rf /opt/sonarqube/web/*
COPY --from=webapp-builder --chown=sonarqube:root ${WORKDIR}/apps/sq-server/build/webapp /opt/sonarqube/web
RUN chmod -R 550 /opt/sonarqube/web

ENV PLUGIN_VERSION=${PLUGIN_VERSION}

#ADD --chown=sonarqube:root https://github.com/sbaudoin/sonar-ansible/releases/download/v2.5.1/sonar-ansible-plugin-2.5.1.jar /opt/sonarqube/extensions/plugins/
#ADD --chown=sonarqube:root https://github.com/C4tWithShell/community-rust/releases/download/v0.2.7/community-rust-plugin-0.2.7.jar /opt/sonarqube/extensions/plugins/
#ADD --chown=sonarqube:root https://github.com/sbaudoin/sonar-yaml/releases/download/v1.9.1/sonar-yaml-plugin-1.9.1.jar /opt/sonarqube/extensions/plugins/
ADD --chown=sonarqube:root https://github.com/green-code-initiative/creedengo-php/releases/download/2.1.0/creedengo-php-plugin-2.1.0.jar /opt/sonarqube/extensions/plugins/
ADD --chown=sonarqube:root https://github.com/green-code-initiative/creedengo-python/releases/download/2.3.0/creedengo-python-plugin-2.3.0.jar /opt/sonarqube/extensions/plugins/
ADD --chown=sonarqube:root https://github.com/SonarOpenCommunity/sonar-cxx/releases/download/cxx-2.2.2/sonar-cxx-plugin-2.2.2.1409.jar /opt/sonarqube/extensions/plugins/

USER sonarqube
ENV SONAR_WEB_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar=web"
ENV SONAR_CE_JAVAADDITIONALOPTS="-javaagent:/opt/sonarqube/extensions/plugins/sonarqube-community-branch-plugin-${PLUGIN_VERSION}.jar=ce"
