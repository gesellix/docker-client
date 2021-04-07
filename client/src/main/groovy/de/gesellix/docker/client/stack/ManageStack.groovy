package de.gesellix.docker.client.stack

interface ManageStack {

  // see docker/docker/cli/compose/convert/compose.go:14
  static final String LabelNamespace = "com.docker.stack.namespace"

//    deploy      Deploy a new stack or update an existing stack

  void stackDeploy(String namespace, DeployStackConfig deployConfig, DeployStackOptions options)

//    ls          List stacks

  Collection<Stack> lsStacks()

//    ps          List the tasks in the stack

  def stackPs(String namespace)

  def stackPs(String namespace, Map filters)

//    rm          Remove the stack

  void stackRm(String namespace)

//    services    List the services in the stack

  def stackServices(String namespace)

  def stackServices(String namespace, Map filters)
}
