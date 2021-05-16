package de.gesellix.docker.client

import de.gesellix.docker.engine.api.ConfigApi
import de.gesellix.docker.engine.api.ContainerApi
import de.gesellix.docker.engine.api.DistributionApi
import de.gesellix.docker.engine.api.ExecApi
import de.gesellix.docker.engine.api.ImageApi
import de.gesellix.docker.engine.api.NetworkApi
import de.gesellix.docker.engine.api.NodeApi
import de.gesellix.docker.engine.api.PluginApi
import de.gesellix.docker.engine.api.SecretApi
import de.gesellix.docker.engine.api.ServiceApi
import de.gesellix.docker.engine.api.SessionApi
import de.gesellix.docker.engine.api.SwarmApi
import de.gesellix.docker.engine.api.SystemApi
import de.gesellix.docker.engine.api.TaskApi
import de.gesellix.docker.engine.api.VolumeApi

class TypeSafeDockerClientImpl : TypeSafeDockerClient {

  val configApi: ConfigApi = ConfigApi()
  val containerApi: ContainerApi = ContainerApi()
  val distributionApi: DistributionApi = DistributionApi()
  val execApi: ExecApi = ExecApi()
  val imageApi: ImageApi = ImageApi()
  val networkApi: NetworkApi = NetworkApi()
  val nodeApi: NodeApi = NodeApi()
  val pluginApi: PluginApi = PluginApi()
  val secretApi: SecretApi = SecretApi()
  val serviceApi: ServiceApi = ServiceApi()
  val sessionApi: SessionApi = SessionApi()
  val swarmApi: SwarmApi = SwarmApi()
  val systemApi: SystemApi = SystemApi()
  val taskApi: TaskApi = TaskApi()
  val volumeApi: VolumeApi = VolumeApi()
}
