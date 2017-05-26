FROM anapsix/alpine-java

RUN apk add --update nodejs
RUN apk add --update zip

ADD stage /
WORKDIR /opt/docker
RUN ["chown", "-R", "daemon", "."]
USER daemon
ENTRYPOINT ["bin/ellipsis", "-Dconfig.resource=production.conf"]
CMD []
EXPOSE 9000
