FROM golang

#ARG CODE=.
#ENV CODE $CODE
#CMD [ "echo $API_CODE" ]

WORKDIR /go/src/tridex/restapi
COPY . .
RUN set -x && \ 
    go get -d -v . && \
    CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o app .

FROM scratch
WORKDIR /root/
COPY --from=0 /go/src/tridex/restapi .
EXPOSE 3000
CMD ["./app"]  
