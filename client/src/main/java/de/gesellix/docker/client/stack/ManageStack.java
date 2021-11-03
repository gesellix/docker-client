package de.gesellix.docker.client.stack;

import java.util.Collection;
import java.util.Map;

public interface ManageStack {

  // see docker/docker/cli/compose/convert/compose.go:14
  String LabelNamespace = "com.docker.stack.namespace";

  void stackDeploy(String namespace, DeployStackConfig deployConfig, DeployStackOptions options);

  Collection<Stack> lsStacks();

  Object stackPs(String namespace);

  Object stackPs(String namespace, Map filters);

  void stackRm(String namespace);

  Object stackServices(String namespace);

  Object stackServices(String namespace, Map filters);
}
