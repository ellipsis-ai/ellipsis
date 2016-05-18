FROM java:8
ADD stage /
WORKDIR /opt/docker
RUN ["chown", "-R", "daemon", "."]
USER daemon
ENTRYPOINT ["bin/ellipsis", "-Dconfig.resource=docker.conf"]
CMD []
EXPOSE 9000
