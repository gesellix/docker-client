FROM alpine:edge

# usage:
# - build: docker build -t socat .
# - run w/ explicit port mapping: docker run --rm -it -p 2375:2375 -v /var/run/docker.sock:/var/run/docker.sock socat
# - run w/ dynamic port mapping:  docker run --rm -it -P -v /var/run/docker.sock:/var/run/docker.sock socat

EXPOSE 2375
RUN apk add -U socat
ENTRYPOINT [ "socat" ]
CMD [ "TCP-LISTEN:2375,reuseaddr,fork", "UNIX-CLIENT:/var/run/docker.sock" ]
