package de.gesellix.docker.client.testutil;

import de.gesellix.docker.client.TypeSafeDockerClientImpl;
import de.gesellix.docker.engine.api.SwarmApi;
import de.gesellix.docker.engine.api.SystemApi;
import de.gesellix.docker.remote.api.LocalNodeState;
import de.gesellix.docker.remote.api.SwarmInitRequest;

public class SwarmUtil {

  SwarmApi swarmApi;
  SystemApi systemApi;

  public SwarmUtil(TypeSafeDockerClientImpl dockerClient) {
    this.swarmApi = dockerClient.getSwarmApi();
    this.systemApi = dockerClient.getSystemApi();
  }

  public void runWithInactiveSwarm(Runnable action) {
    LocalNodeState previousState = ensureInactiveSwarm();
    try {
      action.run();
    }
    finally {
      if (previousState != LocalNodeState.Inactive) {
        ensureActiveSwarm();
      }
    }
  }

  public void runWithActiveSwarm(Runnable action) {
    LocalNodeState previousState = ensureActiveSwarm();
    try {
      action.run();
    }
    finally {
      if (previousState != LocalNodeState.Active) {
        ensureInactiveSwarm();
      }
    }
  }

  LocalNodeState ensureInactiveSwarm() {
    LocalNodeState currentState = null;
    try {
      currentState = systemApi.systemInfo().getSwarm().getLocalNodeState();
      if (currentState != LocalNodeState.Inactive) {
        swarmApi.swarmLeave(true);
      }
    }
    catch (Exception ignored) {
      //
    }
    return currentState;
  }

  LocalNodeState ensureActiveSwarm() {
    LocalNodeState currentState = null;
    try {
      currentState = systemApi.systemInfo().getSwarm().getLocalNodeState();
      if (currentState != LocalNodeState.Active) {
        swarmApi.swarmInit(new SwarmInitRequest("0.0.0.0:2377", "127.0.0.1", null, null, null, false, null, null));
      }
    }
    catch (Exception ignored) {
      //
    }
    return currentState;
  }
}
