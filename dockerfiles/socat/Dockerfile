FROM alpine:edge

# usage:
# - build: docker build -t socat .
# - run w/ explicit port mapping: docker run --rm -it -p 2375:2375 -v /var/run/docker.sock:/var/run/docker.sock socat
# - run w/ dynamic port mapping:  docker run --rm -it -P -v /var/run/docker.sock:/var/run/docker.sock socat

# enable debug mode to watch the traffic:
# https://blog.mikesir87.io/2018/10/using-socat-to-see-docker-socket-traffic/
#
# tl;dr
#     socat -d -v -d TCP-L:2375,fork UNIX:/var/run/docker.sock
#     export DOCKER_HOST=localhost:2375
#
# any Docker commands
#     docker container run --rm -dp 80:80 nginx

EXPOSE 2375
RUN apk add -U socat
ENTRYPOINT [ "socat" ]
CMD [ "TCP-LISTEN:2375,reuseaddr,fork", "UNIX-CLIENT:/var/run/docker.sock" ]
