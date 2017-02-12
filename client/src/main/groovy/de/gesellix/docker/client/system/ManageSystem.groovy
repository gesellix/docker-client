package de.gesellix.docker.client.system

import de.gesellix.docker.client.DockerAsyncCallback

interface ManageSystem {

//    df          Show docker disk usage

    def systemDf()

    def systemDf(query)

//    events      Get real time events from the server

    def events(DockerAsyncCallback callback)

    def events(DockerAsyncCallback callback, Map query)

    def ping()

    def version()

//    info        Display system-wide information

    def info()

//    TODO prune       Remove unused data

}
