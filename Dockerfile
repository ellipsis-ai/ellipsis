FROM ubuntu:16.04
MAINTAINER Matteo Melani <m@ellipsis.ai>

# Locales
RUN locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

# Fix sh
RUN rm /bin/sh && ln -s /bin/bash /bin/sh

# Basic
RUN apt-get update
RUN apt-get install -y apt-utils
RUN DEBIAN_FRONTEND=noninteractive apt-get install -y sudo
RUN DEBIAN_FRONTEND=noninteractive apt-get -y install software-properties-common

# Node and Npm
RUN apt-get install -y zip nodejs npm
RUN ln -s /usr/bin/nodejs /usr/bin/node
RUN npm install -g npm
RUN npm install -g n

# Java
RUN \
  echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  add-apt-repository -y ppa:webupd8team/java && \
  apt-get update && \
  apt-get install -y oracle-java8-installer && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk8-installer

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle

# User
RUN groupadd ellipsis
RUN useradd ellipsis -m -g ellipsis -s /bin/bash
RUN passwd -d -u ellipsis
RUN echo "ellipsis ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/ellipsis
RUN chmod 0440 /etc/sudoers.d/ellipsis
RUN mkdir /home/ellipsis/code
RUN chown ellipsis:ellipsis /home/ellipsis/code
RUN echo "export PATH=$PATH:/opt/activator/bin" >> /home/ellipsis/.bashrc
RUN echo "export _JAVA_OPTIONS='-Duser.home=/home/ellipsis/code'" >> /home/ellipsis/.bashrc

# Curl
RUN apt-get update && apt-get install -y curl && apt-get install -y git

# Change user, launch bash
USER ellipsis
WORKDIR /home/ellipsis

# Activator
RUN curl -O http://downloads.typesafe.com/typesafe-activator/1.3.10/typesafe-activator-1.3.10.zip
RUN unzip typesafe-activator-1.3.10.zip -d /home/ellipsis && rm typesafe-activator-1.3.10.zip
ENV PATH $PATH:/home/ellipsis/activator-dist-1.3.10/bin

WORKDIR /home/ellipsis/code
