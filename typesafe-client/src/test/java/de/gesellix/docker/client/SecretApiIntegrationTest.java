package de.gesellix.docker.client;

import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.client.testutil.SwarmUtil;
import de.gesellix.docker.engine.api.SecretApi;
import de.gesellix.docker.engine.model.IdResponse;
import de.gesellix.docker.engine.model.LocalNodeState;
import de.gesellix.docker.engine.model.Secret;
import de.gesellix.docker.engine.model.SecretSpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static de.gesellix.docker.client.testutil.Constants.LABEL_KEY;
import static de.gesellix.docker.client.testutil.Constants.LABEL_VALUE;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DockerEngineAvailable(requiredSwarmMode = LocalNodeState.Active)
class SecretApiIntegrationTest {

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  SecretApi secretApi;

  IdResponse defaultSecret;

  SwarmUtil swarmUtil;

  @BeforeEach
  public void setup() {
    secretApi = typeSafeDockerClient.getSecretApi();

    swarmUtil = new SwarmUtil(typeSafeDockerClient);

    String encoded = Base64.getEncoder().encodeToString("secret-data".getBytes());
    defaultSecret = secretApi.secretCreate(new SecretSpec("secret-name", Collections.emptyMap(), encoded, null, null));
  }

  @AfterEach
  public void cleanup() {
    if (defaultSecret != null) {
      secretApi.secretDelete(defaultSecret.getId());
    }
  }

  @Test
  public void secretCreate() {
    String encoded = Base64.getEncoder().encodeToString("secret-data".getBytes());
    IdResponse response = secretApi.secretCreate(new SecretSpec("my-secret", Collections.emptyMap(), encoded, null, null));
    assertTrue(response.getId().matches("\\w{5,}"));

    secretApi.secretDelete(response.getId());
  }

  @Test
  public void secretDelete() {
    String encoded = Base64.getEncoder().encodeToString("secret-data".getBytes());
    IdResponse response = secretApi.secretCreate(new SecretSpec("my-secret", Collections.emptyMap(), encoded, null, null));

    assertDoesNotThrow(() -> secretApi.secretDelete(response.getId()));
  }

  @Test
  public void secretInspect() {
    Secret inspect = secretApi.secretInspect(defaultSecret.getId());
    assertEquals("secret-name", inspect.getSpec().getName());
    assertNull(inspect.getSpec().getData());
  }

  @Test
  public void secretList() {
    List<Secret> secrets = secretApi.secretList(null);
    Stream<Secret> filtered = secrets.stream().filter(c -> Objects.equals(c.getID(), defaultSecret.getId()));
    assertEquals(defaultSecret.getId(), filtered.findFirst().orElse(new Secret()).getID());
  }

  @Test
  public void secretUpdate() {
    Secret inspect = secretApi.secretInspect(defaultSecret.getId());
    SecretSpec secretSpec = inspect.getSpec();
    assertNotNull(secretSpec);
    assertDoesNotThrow(() -> secretApi.secretUpdate(defaultSecret.getId(), inspect.getVersion().getIndex(), new SecretSpec(secretSpec.getName(), singletonMap(LABEL_KEY, LABEL_VALUE), secretSpec.getData(), secretSpec.getDriver(), secretSpec.getTemplating())));
  }
}
