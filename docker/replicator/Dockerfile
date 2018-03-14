FROM node:alpine

RUN mkdir -p /opt/replicator
WORKDIR /opt/replicator
RUN mkdir bin
RUN mkdir etc
RUN mkdir lib

COPY ./bin ./bin
COPY ./etc ./etc
COPY ./lib ./lib
COPY ./package.json .

COPY ./entrypoint.sh /

RUN npm install

RUN chmod 0755 ./bin/genesis-replicator
RUN chmod 0755 ./etc/example.sh
RUN chmod 0755 /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
