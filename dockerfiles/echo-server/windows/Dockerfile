FROM golang:1.17-nanoserver AS build-backend
LABEL builder=true

ENV CGO_ENABLED=0
ENV GO111MODULE=on

COPY ./src /app
RUN cd /app && go get -d \
 && go build \
    -a \
    -ldflags "-s -w -extldflags \"-static\"" \
    -o /bin/main main.go \
 && go build \
    -a \
    -ldflags "-s -w -extldflags \"-static\"" \
    -o /bin/cat cat.go

FROM mcr.microsoft.com/windows/nanoserver:1809
#FROM mcr.microsoft.com/windows/nanoserver:1903
#FROM mcr.microsoft.com/windows/nanoserver:20H2

ENV LANG=C.UTF-8
ENV SERVER_PORT=8080

COPY ./gattaca.txt /gattaca.txt

EXPOSE $SERVER_PORT
ENTRYPOINT [ "/main", "-listen-addr=0.0.0.0:${SERVER_PORT}" ]
CMD [ ]

COPY --from=build-backend /bin/main /main
COPY --from=build-backend /bin/cat /cat
