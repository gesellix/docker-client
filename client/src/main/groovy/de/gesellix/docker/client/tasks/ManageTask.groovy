package de.gesellix.docker.client.tasks

import de.gesellix.docker.client.DockerResponse

interface ManageTask {

    DockerResponse tasks()

    DockerResponse tasks(query)

    def inspectTask(name)
}
