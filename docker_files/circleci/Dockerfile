FROM frolvlad/alpine-scala:2.12
MAINTAINER Matteo Melani <m@ellipsis.ai>

# Install activator
ENV ACTIVATOR_VERSION 1.3.10
RUN apk add --update bash curl openssl ca-certificates && \
  curl -L -o /tmp/activator.zip \
    https://downloads.typesafe.com/typesafe-activator/${ACTIVATOR_VERSION}/typesafe-activator-${ACTIVATOR_VERSION}-minimal.zip && \
  mkdir -p /opt/activator && \
  unzip /tmp/activator.zip -d /opt/activator && \
  rm /tmp/activator.zip && \
  chmod +x /opt/activator/activator-${ACTIVATOR_VERSION}-minimal/bin/activator && \
  ln -s /opt/activator/activator-${ACTIVATOR_VERSION}-minimal/bin/activator /usr/bin/activator && \
  rm -rf /tmp/* /var/cache/apk/*

# Git
RUN apk add --no-cache git openssh

# Node and Npm
RUN apk add --update nodejs
RUN npm install -g eslint-plugin-react
RUN npm install -g eslint
RUN npm install -g jest

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle


RUN addgroup -S circleci && adduser -S -g circleci circleci
RUN passwd -d -u circleci
RUN echo "circleci ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers

USER circleci
