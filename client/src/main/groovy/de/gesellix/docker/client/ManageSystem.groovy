package de.gesellix.docker.client

interface ManageSystem {

//    df          Show docker disk usage

    def systemDf()

    def systemDf(query)

//    events      Get real time events from the server

    def events(DockerAsyncCallback callback)

    def events(DockerAsyncCallback callback, query)

//    info        Display system-wide information

    def info()

//    TODO prune       Remove unused data

}
