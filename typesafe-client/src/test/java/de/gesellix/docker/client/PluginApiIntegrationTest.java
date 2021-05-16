package de.gesellix.docker.client;

import de.gesellix.docker.client.testutil.DisabledIfDaemonOnWindowsOs;
import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.engine.api.PluginApi;
import de.gesellix.docker.engine.model.Plugin;
import de.gesellix.docker.engine.model.PluginPrivilege;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DockerEngineAvailable
class PluginApiIntegrationTest {

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  PluginApi pluginApi;

  @BeforeEach
  public void setup() {
    pluginApi = typeSafeDockerClient.getPluginApi();
  }

  @DisabledIfDaemonOnWindowsOs
  @Test
  public void pluginList() {
    List<Plugin> plugins = pluginApi.pluginList(null);
    assertNotNull(plugins);
  }

  @DisabledIfDaemonOnWindowsOs
  @Test
  public void pluginPrivileges() {
    List<PluginPrivilege> privileges = pluginApi.getPluginPrivileges("vieux/sshfs");
    assertEquals("host", privileges.stream().filter((p) -> Objects.equals(p.getName(), "network")).findFirst().get().getValue().get(0));
  }
}
