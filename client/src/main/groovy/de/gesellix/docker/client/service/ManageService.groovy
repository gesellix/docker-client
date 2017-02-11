package de.gesellix.docker.client.service

interface ManageService {

//    create      Create a new service

    def createService(config)

//    inspect     Display detailed information on one or more services

    def inspectService(name)

//    TODO logs        Fetch the logs of a service

//    ls          List services

    def services()

    def services(query)

//    ps          List the tasks of a service

    def tasksOfService(service)

    def tasksOfService(service, query)

//    rm          Remove one or more services

    def rmService(name)

//    scale       Scale one or multiple replicated services

    def scaleService(name, int replicas)

//    update      Update a service

    def updateService(name, query, config)

}
