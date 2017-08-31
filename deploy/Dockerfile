FROM anapsix/alpine-java

RUN apk add --update nodejs
RUN apk add --update zip

ADD stage /

ENV APP_LOG_FILE="/var/log/ellipsis"
RUN mkdir -p ${APP_LOG_FILE} && chown -R daemon:daemon ${APP_LOG_FILE}

WORKDIR /opt/docker

# Install New Relic Agent
RUN apk add curl
RUN curl https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip -z \
      ${}/newrelic-java.zip -o ${DIR}/newrelic-java.zip --silent --location --write-out %{http_code}

RUN chown -R daemon:daemon .
USER daemon
ENTRYPOINT ["bin/ellipsis", "-Dconfig.resource=production.conf", "-Dlogger.resource=logback.prod.xml"]
CMD []
EXPOSE 9000
