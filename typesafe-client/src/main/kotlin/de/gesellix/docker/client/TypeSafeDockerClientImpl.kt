package de.gesellix.docker.client

import de.gesellix.docker.remote.api.client.ConfigApi
import de.gesellix.docker.remote.api.client.ContainerApi
import de.gesellix.docker.remote.api.client.DistributionApi
import de.gesellix.docker.remote.api.client.ExecApi
import de.gesellix.docker.remote.api.client.ImageApi
import de.gesellix.docker.remote.api.client.NetworkApi
import de.gesellix.docker.remote.api.client.NodeApi
import de.gesellix.docker.remote.api.client.PluginApi
import de.gesellix.docker.remote.api.client.SecretApi
import de.gesellix.docker.remote.api.client.ServiceApi
import de.gesellix.docker.remote.api.client.SessionApi
import de.gesellix.docker.remote.api.client.SwarmApi
import de.gesellix.docker.remote.api.client.SystemApi
import de.gesellix.docker.remote.api.client.TaskApi
import de.gesellix.docker.remote.api.client.VolumeApi

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
