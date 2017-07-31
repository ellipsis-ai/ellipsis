FROM anapsix/alpine-java

RUN apk add --update nodejs
RUN apk add --update zip

ADD stage /
WORKDIR /opt/docker
RUN ["chown", "-R", "daemon", "."]
USER daemon
ENTRYPOINT ["bin/ellipsis", "-Dconfig.resource=production.conf", "-Dlogger.resource=logback.prod.xml"]
CMD []
EXPOSE 9000
