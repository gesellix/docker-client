package de.gesellix.docker.client;

import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.client.testutil.SwarmUtil;
import de.gesellix.docker.engine.api.SwarmApi;
import de.gesellix.docker.engine.api.SystemApi;
import de.gesellix.docker.engine.model.LocalNodeState;
import de.gesellix.docker.engine.model.Swarm;
import de.gesellix.docker.engine.model.SwarmInitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DockerEngineAvailable
class SwarmApiIntegrationTest {

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  SwarmApi swarmApi;
  SystemApi systemApi;

  SwarmUtil swarmUtil;

  @BeforeEach
  public void setup() {
    swarmApi = typeSafeDockerClient.getSwarmApi();
    systemApi = typeSafeDockerClient.getSystemApi();

    swarmUtil = new SwarmUtil(typeSafeDockerClient);
  }

  @Test
  public void swarmLocalState() {
    swarmUtil.runWithInactiveSwarm(() -> {
      assertEquals(LocalNodeState.Inactive, systemApi.systemInfo().getSwarm().getLocalNodeState());
    });
  }

  @Test
  public void swarmInit() {
    swarmUtil.runWithInactiveSwarm(() -> {
      String initResult = swarmApi.swarmInit(new SwarmInitRequest("0.0.0.0:2377", "127.0.0.1", null, null, null, false, null, null));
      assertTrue(initResult.matches("\\w+"));
    });
  }

  @Test
  public void swarmLeave() {
    swarmUtil.runWithActiveSwarm(() -> {
      assertDoesNotThrow(() -> swarmApi.swarmLeave(true));
    });
  }

  @Test
  public void swarmInspect() {
    swarmUtil.runWithActiveSwarm(() -> {
      Swarm swarmInspect = swarmApi.swarmInspect();
      assertEquals("default", swarmInspect.getSpec().getName());
    });
  }

  @Test
  public void swarmUnlockKey() {
    swarmUtil.runWithActiveSwarm(() -> assertDoesNotThrow(swarmApi::swarmUnlockkey));
  }
}
