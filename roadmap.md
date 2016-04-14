# Supported Features

*feature set based on the [Docker Remote API v1.23](https://docs.docker.com/engine/reference/api/docker_remote_api_v1.23/)*

Since the Docker remote api tends to be backwards compatible,
the Docker-Client currently supports most other api versions, too.

For most use cases the Docker-Client also works with the Docker Swarm API, but the same
restrictions and changed behaviour apply like described in the
official [Swarm API docs](https://docs.docker.com/swarm/api/swarm-api/).

This project tends to support most api endpoints, but it always depends on free time. If you're missing a feature, please file
a [new issue](https://github.com/gesellix/docker-client/issues) or a [pull request](https://github.com/gesellix/docker-client/pulls)
and we'll add it as soon as possible. If you're looking for another Java based Docker library with similar feature set,
check out the project at [Java Docker API Client](https://github.com/docker-java/docker-java).

## Containers

* [x] `docker ps`: List containers
* [x] `docker create`: Create a container
* [x] `docker inspect <container>`: Inspect a container
* [x] `docker top <container>`: List processes running inside a container
* [x] `docker logs <container>`: Get container logs
* [x] `docker diff <container>`: Inspect changes on a container's filesystem
* [x] `docker export <container>`: Export a container
* [x] `docker stats <container>`: Get container stats based on resource usage
* [x] Resize a container TTY
* [x] `docker start <container>`: Start a container
* [x] `docker stop <container>`: Stop a container
* [x] `docker restart <container>`: Restart a container
* [x] `docker kill <container>`: Kill a container
* [x] `docker rename <container>`: Rename a container
* [x] `docker pause <container>`: Pause a container
* [x] `docker unpause <container>`: Unpause a container
* [x] `docker attach <container>`: Attach to a container - _interactive tty via socket hijacking currently not supported_
* [x] Attach to a container (websocket)
* [x] `docker wait <container>`: Wait a container
* [x] `docker rm <container>`: Remove a container
* [x] `docker cp <container>:<path> <hostpath>`: Copy files or folders from a container - _deprecated_
* [x] Retrieve information about files and folders in a container
* [x] `docker cp <container>:<path> <hostpath>`: Get an archive of a filesystem resource in a container
* [x] `docker cp <hostpath> <container>:<path>`: Extract an archive of files or folders to a directory in a container
* [x] `docker update <container> [<container>...]`: Update resources of one or more containers

## Images

* [x] `docker images`: List Images
* [x] `docker build`: Build image from a Dockerfile
* [x] `docker pull`: Create an image (from the registry)
* [x] `docker import`: Create an image (by import from url)
* [x] `docker import`: Create an image (by import from stream)
* [x] `docker inspect <image>`: Inspect an image
* [x] `docker history <image>`: Get the history of an image
* [x] `docker push <image>`: Push an image on the registry
* [x] `docker tag <image> <repository>`: Tag an image into a repository
* [x] `docker rmi <image>`: Remove an image
* [x] `docker search <term>`: Search images

## Misc

* [x] Check auth configuration
* [x] `docker info`: Display system-wide information
* [x] `docker version`: Show the docker version information
* [x] Ping the docker server
* [x] `docker commit <container>`: Create a new image from a container's changes
* [x] `docker events`: Monitor Docker's events
* [x] `docker save <image>`: Get a tarball containing all images in a repository
* [x] `docker save <image> [<image> ...]`: Get a tarball containing all images.
* [x] `docker load`: Load a tarball with a set of images and tags into docker
* [x] Exec Create
* [x] Exec Start (`docker exec <container> <command>`) - _interactive tty via socket hijacking currently not supported_
* [x] Exec Resize
* [x] Exec Inspect

## Volumes

* [x] `docker volume ls`: List volumes from all volume drivers
* [x] `docker volume create`: Create a volume
* [x] `docker volume inspect`: Return low-level information on a volume
* [x] `docker volume rm`: Remove a volume

## Networks

* [x] `docker network ls`: Lists all networks
* [x] `docker network inspect`: Display detailed information on a network
* [x] `docker network create`: Create a new network
* [x] `docker network connect`: Connect a container to a network
* [x] `docker network disconnect`: Disconnect a container from a network
* [x] `docker network rm`: Remove a network
