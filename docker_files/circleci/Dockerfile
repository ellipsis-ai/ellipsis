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
RUN npm install eslint

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

# User
RUN addgroup -S ellipsis && adduser -S -g ellipsis ellipsis
RUN passwd -d -u ellipsis
RUN echo "ellipsis ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers
RUN mkdir /home/ellipsis/code
RUN chown ellipsis:ellipsis /home/ellipsis/code
# # RUN echo "export PATH=$PATH:/opt/activator/bin" >> /home/ellipsis/.bashrc
# RUN echo "export _JAVA_OPTIONS='-Duser.home=/home/ellipsis/code'" >> /home/ellipsis/.bashrc
#
#
# Change user, launch bash
USER ellipsis

WORKDIR /home/ellipsis/code
