package de.gesellix.docker.client;

import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.engine.api.ConfigApi;
import de.gesellix.docker.remote.api.Config;
import de.gesellix.docker.remote.api.ConfigSpec;
import de.gesellix.docker.remote.api.IdResponse;
import de.gesellix.docker.remote.api.LocalNodeState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DockerEngineAvailable(requiredSwarmMode = LocalNodeState.Active)
class ConfigApiIntegrationTest {

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  ConfigApi configApi;

  IdResponse defaultConfig;

  @BeforeEach
  public void setup() {
    configApi = typeSafeDockerClient.getConfigApi();

    String encoded = Base64.getEncoder().encodeToString("config-data".getBytes());
    defaultConfig = configApi.configCreate(new ConfigSpec("config-name", Collections.emptyMap(), encoded, null));
  }

  @AfterEach
  public void cleanup() {
    configApi.configDelete(defaultConfig.getId());
  }

  @Test
  public void configCreate() {
    String encoded = Base64.getEncoder().encodeToString("config-data".getBytes());
    IdResponse response = configApi.configCreate(new ConfigSpec("my-config", Collections.emptyMap(), encoded, null));
    assertTrue(response.getId().matches("\\w{5,}"));

    configApi.configDelete(response.getId());
  }

  @Test
  public void configDelete() {
    String encoded = Base64.getEncoder().encodeToString("config-data".getBytes());
    IdResponse response = configApi.configCreate(new ConfigSpec("my-config", Collections.emptyMap(), encoded, null));

    assertDoesNotThrow(() -> configApi.configDelete(response.getId()));
  }

  @Test
  public void configInspect() {
    Config inspect = configApi.configInspect(defaultConfig.getId());
    assertEquals("config-name", inspect.getSpec().getName());
    assertEquals("config-data", new String(Base64.getDecoder().decode(inspect.getSpec().getData())));
  }

  @Test
  public void configList() {
    List<Config> configs = configApi.configList(null);
    Stream<Config> filtered = configs.stream().filter(c -> Objects.equals(c.getID(), defaultConfig.getId()));
    assertEquals(defaultConfig.getId(), filtered.findFirst().orElse(new Config()).getID());
  }

  @Test
  public void configUpdate() {
    Config inspect = configApi.configInspect(defaultConfig.getId());
    ConfigSpec configSpec = inspect.getSpec();
    assertNotNull(configSpec);
    assertDoesNotThrow(() -> configApi.configUpdate(defaultConfig.getId(), inspect.getVersion().getIndex(), new ConfigSpec(configSpec.getName(), Collections.singletonMap("foo", "bar"), configSpec.getData(), configSpec.getTemplating())));
  }
}
