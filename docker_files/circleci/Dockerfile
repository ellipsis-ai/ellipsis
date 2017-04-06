FROM frolvlad/alpine-scala:2.12
MAINTAINER Matteo Melani <m@ellipsis.ai>



# Install activator and a bunch of basic tools
# tar and gzip are used by circlci for the restore_cache
ENV ACTIVATOR_VERSION 1.3.10
RUN apk add --update bash curl tar gzip openssl ca-certificates && \
  curl -L -o /tmp/activator.zip \
    https://downloads.typesafe.com/typesafe-activator/${ACTIVATOR_VERSION}/typesafe-activator-${ACTIVATOR_VERSION}-minimal.zip && \
  mkdir -p /opt/activator && \
  unzip /tmp/activator.zip -d /opt/activator && \
  rm /tmp/activator.zip && \
  chmod +x /opt/activator/activator-${ACTIVATOR_VERSION}-minimal/bin/activator && \
  ln -s /opt/activator/activator-${ACTIVATOR_VERSION}-minimal/bin/activator /usr/bin/activator && \
  rm -rf /tmp/* /var/cache/apk/*

# Git
RUN apk add --no-cache git openssh zip

# Node and Npm
RUN apk add --update nodejs

# Docker and docker compose
# RUN apk update && apk add docker make py-pip && pip install docker-compose
RUN apk update && apk add docker

# AWS CLI tools
RUN apk --no-cache update && \
    apk --no-cache add python py-pip py-setuptools ca-certificates && \
    pip --no-cache-dir install awscli && \
    rm -rf /var/cache/apk/*

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle


RUN addgroup -S circleci && adduser -S -g circleci circleci
RUN passwd -d -u circleci
RUN echo "circleci ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers

USER circleci
