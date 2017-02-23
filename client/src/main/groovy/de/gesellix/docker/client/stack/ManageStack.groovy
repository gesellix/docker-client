package de.gesellix.docker.client.stack

interface ManageStack {

//    deploy      Deploy a new stack or update an existing stack

    def stackDeploy(String namespace, DeployStackConfig deployConfig)

//    ls          List stacks

    def lsStacks()

//    ps          List the tasks in the stack

    def stackPs(String namespace)

    def stackPs(String namespace, Map filters)

//    rm          Remove the stack

    def stackRm(String namespace)

//    services    List the services in the stack

    def stackServices(String namespace)

    def stackServices(String namespace, Map filters)
}
