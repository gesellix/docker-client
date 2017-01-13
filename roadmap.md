# Supported Features

*feature set based on the [Docker Engine API v1.24](https://docs.docker.com/engine/reference/api/docker_remote_api_v1.24/)*

Since the Docker engine api tends to be backwards compatible,
the Docker Client currently supports most other api versions, too.

This project tends to support most api endpoints, but it always depends on free time. If you're missing a feature, please file
a [new issue](https://github.com/gesellix/docker-client/issues) or a [pull request](https://github.com/gesellix/docker-client/pulls)
and we'll add it as soon as possible. If you're looking for another Java based Docker library with similar feature set,
check out the project at [Java Docker API Client](https://github.com/docker-java/docker-java).

# Management Commands

## Checkpoints - Manage checkpoints (0/3)

* [ ] `docker checkpoints create`: Create a checkpoint from a running container
* [ ] `docker checkpoints ls`: List checkpoints for a container
* [ ] `docker checkpoints rm`: Remove a checkpoint

## Container - Manage containers (31/32)

* [x] `docker container attach <container>`: Attach to a running container (supports interactive tty)
* [x] Attach to a running container (websocket)
* [x] Resize a container TTY
* [x] `docker container commit <container>`: Create a new image from a container's changes
* [x] `docker container cp <container>:<path> <hostpath>`: Get an archive of a filesystem resource in a container
* [x] `docker container cp <hostpath> <container>:<path>`: Extract an archive of files or folders to a directory in a container
* [x] Retrieve information about files and folders in a container
* [x] `docker container create`: Create a new container
* [x] `docker container diff <container>`: Inspect changes on a container's filesystem
* [x] `docker container exec <container> <command>`: Run a command in a running container
* [x] Exec Start (supports interactive tty)
* [x] Exec Create
* [x] Exec Resize
* [x] Exec Inspect
* [x] `docker container export <container>`: Export a container's filesystem as a tar archive
* [x] `docker container inspect <container>`: Display detailed information on one or more containers
* [x] `docker container kill <container>`: Kill one or more running containers
* [x] `docker container logs <container>`: Fetch the logs of a container
* [x] `docker container ps`: List containers (alias for `ls`, `list`)
* [x] `docker container pause <container>`: Pause all processes within one or more containers
* [ ] `docker container port`: List port mappings or a specific mapping for the container
* [x] `docker container prune`: Remove all stopped containers
* [x] `docker container rename <container>`: Rename a container
* [x] `docker container restart <container>`: Restart one or more containers
* [x] `docker container rm <container>`: Remove one or more containers
* [x] `docker container run`: Run a command in a new container
* [x] `docker container start <container>`: Start one or more stopped containers
* [x] `docker container stats <container>`: Display a live stream of container(s) resource usage statistics
* [x] `docker container stop <container>`: Stop one or more running containers
* [x] `docker container top <container>`: Display the running processes of a container
* [x] `docker container unpause <container>`: Unpause all processes within one or more containers
* [x] `docker container update <container> [<container>...]`: Update configuration of one or more containers
* [x] `docker container wait <container>`: Block until one or more containers stop, then print their exit codes

## Image - Manage images (14/14)

* [x] `docker image build`: Build an image from a Dockerfile
* [x] `docker image history <image>`: Show the history of an image
* [x] `docker image import`: Import the contents from a tarball to create a filesystem image (from stream)
* [x] `docker image import`: Import the contents from a tarball to create a filesystem image (from url)
* [x] `docker image inspect <image>`: Display detailed information on one or more images
* [x] `docker image load`: Load a tarball with a set of images and tags into docker
* [x] `docker image ls`: List Images
* [x] `docker image prune`: Remove unused images
* [x] `docker image pull`: Pull an image or a repository from a registry
* [x] `docker image push <image>`: Push an image or a repository to a registry
* [x] `docker image rm <image>`: Remove one or more images
* [x] `docker image save <image>`: Get a tarball containing all images in a repository
* [x] `docker image save <image> [<image> ...]`: Get a tarball containing all images.
* [x] `docker image tag <image> <repository>`: Create a tag TARGET_IMAGE that refers to SOURCE_IMAGE

## Network - Manage networks (7/7)

* [x] `docker network connect`: Connect a container to a network
* [x] `docker network create`: Create a network
* [x] `docker network disconnect`: Disconnect a container from a network
* [x] `docker network inspect`: Display detailed information on one or more networks
* [x] `docker network ls`: List networks
* [x] `docker network prune`: Remove all unused networks
* [x] `docker network rm`: Remove one or more networks

## Node - Manage Swarm nodes (7/7)

* [x] `docker node demote`: Demote a node from manager in the swarm
* [x] `docker node inspect`: Inspect a node in the swarm
* [x] `docker node ls`: List nodes in the swarm
* [x] `docker node promote`: Promote a node to a manager in the swarm
* [x] `docker node ps`: List tasks running on a node
* [x] `docker node rm`: Remove a node from the swarm
* [x] `docker node update`: Update a node

## Plugin - Manage plugins (0/10)

* [ ] `docker plugin create`: Create a plugin from a rootfs and config `POST /plugins/create`
* [ ] `docker plugin disable`: Disable a plugin `POST /plugins/{name:.*}/disable`
* [ ] `docker plugin enable`: Enable a plugin `POST /plugins/{name:.*}/enable`
* [ ] `docker plugin inspect`: Inspect a plugin `GET /plugins/{name:.*}/json`
* [ ] `docker plugin install`: Install a plugin (equivalent to pull + enable)
* [ ] `docker plugin ls`: List plugins `GET /plugins`
* [ ] Get plugin privileges `GET /plugins/privileges`
* [ ] `docker plugin push`: Push a plugin `POST /plugins/{name:.*}/push`
* [ ] Pull a plugin `POST /plugins/pull`
* [ ] `docker plugin rm`: Remove a plugin `DELETE /plugins/{name:.*}`
* [ ] `docker plugin set`: Change settings for a plugin `POST /plugins/{name:.*}/set`

## Secrets - Manage Docker secrets (4/5)

* [x] `docker secret create`: Create a secret
* [x] `docker secret inspect`: Inspect a secret
* [x] `docker secret ls`: List secrets
* [ ] Update a Secret `POST /secrets/{id}/update`
* [x] `docker secret rm`: Delete a secret

## Service - Manage services (7/8)

* [x] `docker service create`: Create a service
* [x] `docker service inspect`: Return information on the service `<id>`
* [ ] `docker service logs`: Get service logs (GET `/services/{id}/logs`)
* [x] `docker service ls`: List services
* [x] `docker service ps`: List the tasks of a service
* [x] `docker service rm`: Remove a service
* [x] `docker service scale`: Scale one or multiple services
* [x] `docker service update`: Update a service

## Stack - Manage Docker stacks (0/5)

* [ ] `docker stack deploy`: Deploy a new stack or update an existing stack
* [ ] `docker stack ls`: List stacks
* [ ] `docker stack ps`: List the tasks in the stack
* [ ] `docker stack rm`: Remove the stack
* [ ] `docker stack services`: List the services in the stack

## Swarm - Manage Swarm (8/8)

* [x] `docker swarm init`: Initialize a Swarm
* [x] `docker swarm join`: Join a Swarm as a node and/or manager
* [x] `docker swarm join-token`: Manage join tokens
* [x] `docker swarm leave`: Leave a Swarm
* [x] `docker swarm inspect`: Inspect the Swarm
* [x] `docker swarm unlock`: Unlock swarm
* [x] `docker swarm unlock-key`: Manage the unlock key
* [x] `docker swarm update`: Update the Swarm

## Misc, System - Manage Docker (3/4)

* [x] `docker system info`: Display system-wide information
* [x] `docker system df`: Show docker disk usage (GET /system/df)
* [x] `docker system events`: Monitor Docker's events
* [ ] `docker system prune`: Remove unused data

## Volume - Manage volumes (5/5)

* [x] `docker volume create`: Create a volume
* [x] `docker volume inspect`: Display detailed information on one or more volumes
* [x] `docker volume ls`: List volumes from all volume drivers
* [x] `docker volume prune`: Remove all unused volumes
* [x] `docker volume rm`: Remove one or more volumes

# Other Commands

## Misc (4/4)

* [x] `docker search <term>`: Search the Docker Hub for images
* [x] `docker version`: Show the docker version information
* [x] Check auth configuration
* [x] Ping the docker server

## Tasks (2/2)

* [x] List all tasks
* [x] Get details on a task
