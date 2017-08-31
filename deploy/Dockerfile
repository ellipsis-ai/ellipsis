FROM anapsix/alpine-java

RUN apk add --update nodejs zip curl

ADD stage /

ENV APP_LOG_FILE="/var/log/ellipsis"
RUN mkdir -p ${APP_LOG_FILE} && chown -R daemon:daemon ${APP_LOG_FILE}

WORKDIR /opt/docker

# Install New Relic Agent


RUN chown -R daemon:daemon .
USER daemon
ENTRYPOINT ["bin/ellipsis", "-Dconfig.resource=production.conf", "-Dlogger.resource=logback.prod.xml"]
CMD []
EXPOSE 9000
