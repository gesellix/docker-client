package de.gesellix.docker.client.stack;

public class DeployStackOptions {

  private boolean pruneServices = false;
  private boolean sendRegistryAuth = false;

  public boolean getPruneServices() {
    return pruneServices;
  }

  public boolean isPruneServices() {
    return pruneServices;
  }

  public void setPruneServices(boolean pruneServices) {
    this.pruneServices = pruneServices;
  }

  public boolean getSendRegistryAuth() {
    return sendRegistryAuth;
  }

  public boolean isSendRegistryAuth() {
    return sendRegistryAuth;
  }

  public void setSendRegistryAuth(boolean sendRegistryAuth) {
    this.sendRegistryAuth = sendRegistryAuth;
  }
}
