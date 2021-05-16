package de.gesellix.docker.client;

import com.squareup.moshi.Moshi;
import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.client.testutil.TestImage;
import de.gesellix.docker.engine.api.ContainerApi;
import de.gesellix.docker.engine.api.ImageApi;
import de.gesellix.docker.engine.api.NetworkApi;
import de.gesellix.docker.engine.model.ContainerCreateRequest;
import de.gesellix.docker.engine.model.Network;
import de.gesellix.docker.engine.model.NetworkConnectRequest;
import de.gesellix.docker.engine.model.NetworkCreateRequest;
import de.gesellix.docker.engine.model.NetworkCreateResponse;
import de.gesellix.docker.engine.model.NetworkDisconnectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.gesellix.docker.client.testutil.Constants.LABEL_KEY;
import static de.gesellix.docker.client.testutil.Constants.LABEL_VALUE;
import static de.gesellix.docker.client.testutil.Failsafe.removeContainer;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DockerEngineAvailable
class NetworkApiIntegrationTest {

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  private TestImage testImage;

  NetworkApi networkApi;
  ContainerApi containerApi;
  ImageApi imageApi;

  @BeforeEach
  public void setup() {
    networkApi = typeSafeDockerClient.getNetworkApi();
    containerApi = typeSafeDockerClient.getContainerApi();
    imageApi = typeSafeDockerClient.getImageApi();
    testImage = new TestImage(typeSafeDockerClient);
  }

  @Test
  public void networkList() {
    List<Network> networks = networkApi.networkList(null);
    assertFalse(networks.isEmpty());
    Network firstNetwork = networks.get(0);
    assertTrue(firstNetwork.getName().matches("\\w+"));
  }

  @Test
  public void networkInspect() {
    List<Network> networks = networkApi.networkList(null);
    Network firstNetwork = networks.get(0);
    Network network = networkApi.networkInspect(firstNetwork.getId(), true, firstNetwork.getScope());
    assertEquals(firstNetwork.getScope(), network.getScope());
  }

  @Test
  public void networkCreateDelete() {
    NetworkCreateResponse response = networkApi.networkCreate(new NetworkCreateRequest("test-network", null, null, null, null, null, null, null, null, singletonMap(LABEL_KEY, LABEL_VALUE)));

    Map<String, List<String>> filter = new HashMap<>();
    filter.put("label", singletonList(LABEL_KEY));
    String filterJson = new Moshi.Builder().build().adapter(Map.class).toJson(filter);
    List<Network> networks = networkApi.networkList(filterJson);
    Optional<Network> testNetwork = networks.stream().filter((n) -> n.getName().equals("test-network")).findFirst();

    assertTrue(testNetwork.isPresent());
    assertEquals(response.getId(), testNetwork.get().getId());
    networkApi.networkDelete("test-network");
  }

  @Test
  public void networkPrune() {
    networkApi.networkCreate(new NetworkCreateRequest("test-network", null, null, null, null, null, null, null, null, singletonMap(LABEL_KEY, LABEL_VALUE)));

    Map<String, List<String>> filter = new HashMap<>();
    filter.put("label", singletonList(LABEL_KEY));
    String filterJson = new Moshi.Builder().build().adapter(Map.class).toJson(filter);

    networkApi.networkPrune(filterJson);

    List<Network> networks = networkApi.networkList(filterJson);
    Optional<Network> testNetwork = networks.stream().filter((n) -> n.getName().equals("test-network")).findAny();
    assertFalse(testNetwork.isPresent());

    assertDoesNotThrow(() -> networkApi.networkDelete("test-network"));
  }

  @Test
  public void networkConnectDisconnect() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-network-test");

    networkApi.networkCreate(new NetworkCreateRequest("test-network", null, null, null, null, null, null, null, null, singletonMap(LABEL_KEY, LABEL_VALUE)));

    assertDoesNotThrow(() -> networkApi.networkConnect("test-network", new NetworkConnectRequest("container-network-test", null)));
    assertDoesNotThrow(() -> networkApi.networkDisconnect("test-network", new NetworkDisconnectRequest("container-network-test", null)));

    networkApi.networkDelete("test-network");
    removeContainer(typeSafeDockerClient, "container-network-test");
  }
}
