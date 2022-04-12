package de.gesellix.docker.client.stack;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.Service;
import de.gesellix.docker.remote.api.Task;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ManageStack {

  // see docker/docker/cli/compose/convert/compose.go:14
  String LabelNamespace = "com.docker.stack.namespace";

  void stackDeploy(String namespace, DeployStackConfig deployConfig, DeployStackOptions options);

  Collection<Stack> lsStacks();

  EngineResponseContent<List<Task>> stackPs(String namespace);

  EngineResponseContent<List<Task>> stackPs(String namespace, Map filters);

  void stackRm(String namespace);

  EngineResponseContent<List<Service>> stackServices(String namespace);

  EngineResponseContent<List<Service>> stackServices(String namespace, Map filters);
}
