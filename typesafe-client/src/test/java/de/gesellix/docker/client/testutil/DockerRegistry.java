package de.gesellix.docker.client.testutil;

import de.gesellix.docker.client.TypeSafeDockerClientImpl;
import de.gesellix.docker.remote.api.ContainerCreateRequest;
import de.gesellix.docker.remote.api.ContainerCreateResponse;
import de.gesellix.docker.remote.api.ContainerInspectResponse;
import de.gesellix.docker.remote.api.HostConfig;
import de.gesellix.docker.remote.api.PortBinding;

import java.util.List;
import java.util.Objects;

import static de.gesellix.docker.client.testutil.Constants.LABEL_KEY;
import static de.gesellix.docker.client.testutil.Constants.LABEL_VALUE;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

public class DockerRegistry {

  TypeSafeDockerClientImpl dockerClient;
  String registryId;

  public static void main(String[] args) throws InterruptedException {
    DockerRegistry registry = new DockerRegistry(new TypeSafeDockerClientImpl());
    registry.run();
    Thread.sleep(10000);
    registry.rm();
  }

  public DockerRegistry(TypeSafeDockerClientImpl dockerClient) {
    this.dockerClient = dockerClient;
  }

  String getImage() {
//    dockerClient.getSystemApi().systemInfo().getOsType()
    boolean isWindows = Objects.requireNonNull(dockerClient.getSystemApi().systemVersion().getOs()).equalsIgnoreCase("windows");
    return isWindows ? "gesellix/registry:2.7.1-windows" : "registry:2.7.1";
  }

  public void run() {
    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        singletonMap("5000/tcp", emptyMap()),
        false, null, null,
        null,
        null,
        null,
        null,
        getImage(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        new HostConfig(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                       null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                       null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                       null, null, true, null, null, null, null, null, null, null, null, null, null, null, null, null),
        null
    );
    dockerClient.getImageApi().imageCreate(containerCreateRequest.getImage().split(":")[0], null, null, containerCreateRequest.getImage().split(":")[1], null, null, null, null, null);
    ContainerCreateResponse createResponse = dockerClient.getContainerApi().containerCreate(containerCreateRequest, null);
    dockerClient.getContainerApi().containerStart(createResponse.getId(), null);
    registryId = createResponse.getId();
  }

  String address() {
//        String dockerHost = dockerClient.config.dockerHost
//        return dockerHost.replaceAll("^(tcp|http|https)://", "").replaceAll(":\\d+\$", "")

//        def registryContainer = dockerClient.inspectContainer(registryId).content
//        def portBinding = registryContainer.NetworkSettings.Ports["5000/tcp"]
//        return portBinding[0].HostIp as String

    // 'localhost' allows to use the registry without TLS
    return "localhost";
  }

  int port() {
    ContainerInspectResponse registryContainer = dockerClient.getContainerApi().containerInspect(registryId, false);
    List<PortBinding> portBinding = registryContainer.getNetworkSettings().getPorts().get("5000/tcp");
    return Integer.parseInt(portBinding.stream().findFirst().get().getHostPort());
  }

  public String url() {
    return address() + ":" + port();
  }

  public void rm() {
    dockerClient.getContainerApi().containerStop(registryId, null);
    dockerClient.getContainerApi().containerWait(registryId, null);
    dockerClient.getContainerApi().containerDelete(registryId, null, null, null);
  }
}
