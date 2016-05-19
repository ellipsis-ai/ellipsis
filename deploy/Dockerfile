FROM java:8
RUN apt-get update && apt-get upgrade -y
RUN apt-get install -y zip nodejs npm
ADD stage /
RUN ["chown", "-R", "$USER", "~/.npm"]
WORKDIR /opt/docker
RUN ["chown", "-R", "daemon", "."]
USER daemon
ENTRYPOINT ["bin/ellipsis", "-Dconfig.resource=docker.conf"]
CMD []
EXPOSE 9000
