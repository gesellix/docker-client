package de.gesellix.docker.client.testutil;

import de.gesellix.docker.client.TypeSafeDockerClientImpl;

public class Failsafe {

  public static void removeContainer(TypeSafeDockerClientImpl dockerClient, String container) {
    perform(() -> dockerClient.getContainerApi().containerStop(container, 5));
    perform(() -> dockerClient.getContainerApi().containerWait(container, null));
    perform(() -> dockerClient.getContainerApi().containerDelete(container, null, null, null));
  }

  public static void perform(Runnable action) {
    try {
      action.run();
    }
    catch (Exception e) {
      System.out.println("ignoring " + e.getMessage());
    }
  }
}
