package de.gesellix.docker.client.tasks

interface ManageTask {

    def tasks()

    def tasks(query)

    def inspectTask(name)
}
