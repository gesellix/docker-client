package de.gesellix.docker.client.service

import de.gesellix.docker.engine.EngineResponse

interface ManageService {

//    create      Create a new service

    EngineResponse createService(config)

    EngineResponse createService(config, updateOptions)

//    inspect     Display detailed information on one or more services

    EngineResponse inspectService(name)

//    TODO logs        Fetch the logs of a service

//    ls          List services

    EngineResponse services()

    EngineResponse services(query)

//    ps          List the tasks of a service

    def tasksOfService(service)

    def tasksOfService(service, query)

//    rm          Remove one or more services

    def rmService(name)

//    scale       Scale one or multiple replicated services

    def scaleService(name, int replicas)

//    update      Update a service

    EngineResponse updateService(name, query, config)

    EngineResponse updateService(name, query, config, updateOptions)
}
