package de.gesellix.docker.explore

import de.gesellix.docker.engine.DockerClientConfig
import de.gesellix.docker.remote.api.EngineApiClientImpl
import de.gesellix.docker.remote.api.core.Frame
import de.gesellix.docker.remote.api.core.StreamCallback
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Duration
import java.time.temporal.ChronoUnit

@Ignore
class OkDockerClientExplorationTest extends Specification {

  def "local test"() {
    def defaultDockerHost = System.getenv("DOCKER_HOST")
//        defaultDockerHost = "unix:///var/run/docker.sock"
//        defaultDockerHost = "http://192.168.99.100:2376"
//        System.setProperty("docker.cert.path", "/Users/${System.getProperty('user.name')}/.docker/machine/machines/default")
//        System.setProperty("docker.cert.path", "C:\\Users\\${System.getProperty('user.name')}\\.boot2docker\\certs\\boot2docker-vm")
    def client = new EngineApiClientImpl(new DockerClientConfig(defaultDockerHost ?: "http://172.17.42.1:4243/"))

    def response
//        response = client.get("/_ping")
//        println response
//        response = client.get("/version")
//        println response
//        response = client.get("/info")
//        println response
//        response = client.get([path: "/images/json", query: [filters: new JsonBuilder([dangling: ["true"]]).toString()]])
//        println response
//        response = client.get([path: "/containers/json", query: [filters: new JsonBuilder([status: ["exited"]]).toString()]])
//        println response
//        response = client.get("/containers/test/json")
//        println response
//        response = client.post("/containers/test/attach?logs=1&stream=1&stdout=1&stderr=0&tty=false")
//        println response
//        response = client.post("/images/create?fromImage=gesellix%2Ftestimage&tag=latest&registry=")
//        println response
//        response = client.post([path : "/images/create",
//                                query: [fromImage: "gesellix/testimage", tag: "latest", "registry": ""]])
//        println response
//        response = client.get([path : "/events",
//                               async: true])
//        new DockerStreamConsumer(response.stream as InputStream).consume(System.out)
//        response = client.get([path  : "/containers/foo/logs",
//                               query : [stdout: true],
//                               stdout: System.out])
    client.containerApi.containerLogs(
        "foo", true, true, true, 0, null, true, "all",
        new StreamCallback<Frame>() {
          @Override
          void onNext(Frame element) {
            println(element)
          }
        }, Duration.of(10, ChronoUnit.SECONDS).toMillis())

    expect:
    1 == 1
  }
}
