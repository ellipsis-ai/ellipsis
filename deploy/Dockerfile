FROM java:8
RUN apt-get update && apt-get upgrade -y
RUN apt-get install -y zip nodejs npm
RUN ln -s /usr/bin/nodejs /usr/bin/node
RUN npm install -g npm
RUN npm install -g n
ADD stage /
WORKDIR /opt/docker
RUN ["chown", "-R", "daemon", "."]
USER daemon
ENTRYPOINT ["bin/ellipsis", "-Dconfig.resource=production.conf"]
CMD []
EXPOSE 9000
