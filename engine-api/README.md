# org.openapitools.client - Kotlin client library for Docker Engine API

## Requires

* Kotlin 1.4.30
* Gradle 6.8.3

## Features/Implementation Notes

* Supports JSON inputs/outputs, File inputs, and Form inputs.
* Supports collection formats for query parameters: csv, tsv, ssv, pipes.
* Some Kotlin and Java types are fully qualified to avoid conflicts with types defined in OpenAPI definitions.
* Implementation of ApiClient is intended to reduce method counts, specifically to benefit Android targets.

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *http://localhost/v1.41*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*ConfigApi* | [**configCreate**](docs/ConfigApi.md#configcreate) | **POST** /configs/create | Create a config
*ConfigApi* | [**configDelete**](docs/ConfigApi.md#configdelete) | **DELETE** /configs/{id} | Delete a config
*ConfigApi* | [**configInspect**](docs/ConfigApi.md#configinspect) | **GET** /configs/{id} | Inspect a config
*ConfigApi* | [**configList**](docs/ConfigApi.md#configlist) | **GET** /configs | List configs
*ConfigApi* | [**configUpdate**](docs/ConfigApi.md#configupdate) | **POST** /configs/{id}/update | Update a Config
*ContainerApi* | [**containerArchive**](docs/ContainerApi.md#containerarchive) | **GET** /containers/{id}/archive | Get an archive of a filesystem resource in a container
*ContainerApi* | [**containerArchiveInfo**](docs/ContainerApi.md#containerarchiveinfo) | **HEAD** /containers/{id}/archive | Get information about files in a container
*ContainerApi* | [**containerAttach**](docs/ContainerApi.md#containerattach) | **POST** /containers/{id}/attach | Attach to a container
*ContainerApi* | [**containerAttachWebsocket**](docs/ContainerApi.md#containerattachwebsocket) | **GET** /containers/{id}/attach/ws | Attach to a container via a websocket
*ContainerApi* | [**containerChanges**](docs/ContainerApi.md#containerchanges) | **GET** /containers/{id}/changes | Get changes on a container’s filesystem
*ContainerApi* | [**containerCreate**](docs/ContainerApi.md#containercreate) | **POST** /containers/create | Create a container
*ContainerApi* | [**containerDelete**](docs/ContainerApi.md#containerdelete) | **DELETE** /containers/{id} | Remove a container
*ContainerApi* | [**containerExport**](docs/ContainerApi.md#containerexport) | **GET** /containers/{id}/export | Export a container
*ContainerApi* | [**containerInspect**](docs/ContainerApi.md#containerinspect) | **GET** /containers/{id}/json | Inspect a container
*ContainerApi* | [**containerKill**](docs/ContainerApi.md#containerkill) | **POST** /containers/{id}/kill | Kill a container
*ContainerApi* | [**containerList**](docs/ContainerApi.md#containerlist) | **GET** /containers/json | List containers
*ContainerApi* | [**containerLogs**](docs/ContainerApi.md#containerlogs) | **GET** /containers/{id}/logs | Get container logs
*ContainerApi* | [**containerPause**](docs/ContainerApi.md#containerpause) | **POST** /containers/{id}/pause | Pause a container
*ContainerApi* | [**containerPrune**](docs/ContainerApi.md#containerprune) | **POST** /containers/prune | Delete stopped containers
*ContainerApi* | [**containerRename**](docs/ContainerApi.md#containerrename) | **POST** /containers/{id}/rename | Rename a container
*ContainerApi* | [**containerResize**](docs/ContainerApi.md#containerresize) | **POST** /containers/{id}/resize | Resize a container TTY
*ContainerApi* | [**containerRestart**](docs/ContainerApi.md#containerrestart) | **POST** /containers/{id}/restart | Restart a container
*ContainerApi* | [**containerStart**](docs/ContainerApi.md#containerstart) | **POST** /containers/{id}/start | Start a container
*ContainerApi* | [**containerStats**](docs/ContainerApi.md#containerstats) | **GET** /containers/{id}/stats | Get container stats based on resource usage
*ContainerApi* | [**containerStop**](docs/ContainerApi.md#containerstop) | **POST** /containers/{id}/stop | Stop a container
*ContainerApi* | [**containerTop**](docs/ContainerApi.md#containertop) | **GET** /containers/{id}/top | List processes running inside a container
*ContainerApi* | [**containerUnpause**](docs/ContainerApi.md#containerunpause) | **POST** /containers/{id}/unpause | Unpause a container
*ContainerApi* | [**containerUpdate**](docs/ContainerApi.md#containerupdate) | **POST** /containers/{id}/update | Update a container
*ContainerApi* | [**containerWait**](docs/ContainerApi.md#containerwait) | **POST** /containers/{id}/wait | Wait for a container
*ContainerApi* | [**putContainerArchive**](docs/ContainerApi.md#putcontainerarchive) | **PUT** /containers/{id}/archive | Extract an archive of files or folders to a directory in a container
*DistributionApi* | [**distributionInspect**](docs/DistributionApi.md#distributioninspect) | **GET** /distribution/{name}/json | Get image information from the registry
*ExecApi* | [**containerExec**](docs/ExecApi.md#containerexec) | **POST** /containers/{id}/exec | Create an exec instance
*ExecApi* | [**execInspect**](docs/ExecApi.md#execinspect) | **GET** /exec/{id}/json | Inspect an exec instance
*ExecApi* | [**execResize**](docs/ExecApi.md#execresize) | **POST** /exec/{id}/resize | Resize an exec instance
*ExecApi* | [**execStart**](docs/ExecApi.md#execstart) | **POST** /exec/{id}/start | Start an exec instance
*ImageApi* | [**buildPrune**](docs/ImageApi.md#buildprune) | **POST** /build/prune | Delete builder cache
*ImageApi* | [**imageBuild**](docs/ImageApi.md#imagebuild) | **POST** /build | Build an image
*ImageApi* | [**imageCommit**](docs/ImageApi.md#imagecommit) | **POST** /commit | Create a new image from a container
*ImageApi* | [**imageCreate**](docs/ImageApi.md#imagecreate) | **POST** /images/create | Create an image
*ImageApi* | [**imageDelete**](docs/ImageApi.md#imagedelete) | **DELETE** /images/{name} | Remove an image
*ImageApi* | [**imageGet**](docs/ImageApi.md#imageget) | **GET** /images/{name}/get | Export an image
*ImageApi* | [**imageGetAll**](docs/ImageApi.md#imagegetall) | **GET** /images/get | Export several images
*ImageApi* | [**imageHistory**](docs/ImageApi.md#imagehistory) | **GET** /images/{name}/history | Get the history of an image
*ImageApi* | [**imageInspect**](docs/ImageApi.md#imageinspect) | **GET** /images/{name}/json | Inspect an image
*ImageApi* | [**imageList**](docs/ImageApi.md#imagelist) | **GET** /images/json | List Images
*ImageApi* | [**imageLoad**](docs/ImageApi.md#imageload) | **POST** /images/load | Import images
*ImageApi* | [**imagePrune**](docs/ImageApi.md#imageprune) | **POST** /images/prune | Delete unused images
*ImageApi* | [**imagePush**](docs/ImageApi.md#imagepush) | **POST** /images/{name}/push | Push an image
*ImageApi* | [**imageSearch**](docs/ImageApi.md#imagesearch) | **GET** /images/search | Search images
*ImageApi* | [**imageTag**](docs/ImageApi.md#imagetag) | **POST** /images/{name}/tag | Tag an image
*NetworkApi* | [**networkConnect**](docs/NetworkApi.md#networkconnect) | **POST** /networks/{id}/connect | Connect a container to a network
*NetworkApi* | [**networkCreate**](docs/NetworkApi.md#networkcreate) | **POST** /networks/create | Create a network
*NetworkApi* | [**networkDelete**](docs/NetworkApi.md#networkdelete) | **DELETE** /networks/{id} | Remove a network
*NetworkApi* | [**networkDisconnect**](docs/NetworkApi.md#networkdisconnect) | **POST** /networks/{id}/disconnect | Disconnect a container from a network
*NetworkApi* | [**networkInspect**](docs/NetworkApi.md#networkinspect) | **GET** /networks/{id} | Inspect a network
*NetworkApi* | [**networkList**](docs/NetworkApi.md#networklist) | **GET** /networks | List networks
*NetworkApi* | [**networkPrune**](docs/NetworkApi.md#networkprune) | **POST** /networks/prune | Delete unused networks
*NodeApi* | [**nodeDelete**](docs/NodeApi.md#nodedelete) | **DELETE** /nodes/{id} | Delete a node
*NodeApi* | [**nodeInspect**](docs/NodeApi.md#nodeinspect) | **GET** /nodes/{id} | Inspect a node
*NodeApi* | [**nodeList**](docs/NodeApi.md#nodelist) | **GET** /nodes | List nodes
*NodeApi* | [**nodeUpdate**](docs/NodeApi.md#nodeupdate) | **POST** /nodes/{id}/update | Update a node
*PluginApi* | [**getPluginPrivileges**](docs/PluginApi.md#getpluginprivileges) | **GET** /plugins/privileges | Get plugin privileges
*PluginApi* | [**pluginCreate**](docs/PluginApi.md#plugincreate) | **POST** /plugins/create | Create a plugin
*PluginApi* | [**pluginDelete**](docs/PluginApi.md#plugindelete) | **DELETE** /plugins/{name} | Remove a plugin
*PluginApi* | [**pluginDisable**](docs/PluginApi.md#plugindisable) | **POST** /plugins/{name}/disable | Disable a plugin
*PluginApi* | [**pluginEnable**](docs/PluginApi.md#pluginenable) | **POST** /plugins/{name}/enable | Enable a plugin
*PluginApi* | [**pluginInspect**](docs/PluginApi.md#plugininspect) | **GET** /plugins/{name}/json | Inspect a plugin
*PluginApi* | [**pluginList**](docs/PluginApi.md#pluginlist) | **GET** /plugins | List plugins
*PluginApi* | [**pluginPull**](docs/PluginApi.md#pluginpull) | **POST** /plugins/pull | Install a plugin
*PluginApi* | [**pluginPush**](docs/PluginApi.md#pluginpush) | **POST** /plugins/{name}/push | Push a plugin
*PluginApi* | [**pluginSet**](docs/PluginApi.md#pluginset) | **POST** /plugins/{name}/set | Configure a plugin
*PluginApi* | [**pluginUpgrade**](docs/PluginApi.md#pluginupgrade) | **POST** /plugins/{name}/upgrade | Upgrade a plugin
*SecretApi* | [**secretCreate**](docs/SecretApi.md#secretcreate) | **POST** /secrets/create | Create a secret
*SecretApi* | [**secretDelete**](docs/SecretApi.md#secretdelete) | **DELETE** /secrets/{id} | Delete a secret
*SecretApi* | [**secretInspect**](docs/SecretApi.md#secretinspect) | **GET** /secrets/{id} | Inspect a secret
*SecretApi* | [**secretList**](docs/SecretApi.md#secretlist) | **GET** /secrets | List secrets
*SecretApi* | [**secretUpdate**](docs/SecretApi.md#secretupdate) | **POST** /secrets/{id}/update | Update a Secret
*ServiceApi* | [**serviceCreate**](docs/ServiceApi.md#servicecreate) | **POST** /services/create | Create a service
*ServiceApi* | [**serviceDelete**](docs/ServiceApi.md#servicedelete) | **DELETE** /services/{id} | Delete a service
*ServiceApi* | [**serviceInspect**](docs/ServiceApi.md#serviceinspect) | **GET** /services/{id} | Inspect a service
*ServiceApi* | [**serviceList**](docs/ServiceApi.md#servicelist) | **GET** /services | List services
*ServiceApi* | [**serviceLogs**](docs/ServiceApi.md#servicelogs) | **GET** /services/{id}/logs | Get service logs
*ServiceApi* | [**serviceUpdate**](docs/ServiceApi.md#serviceupdate) | **POST** /services/{id}/update | Update a service
*SessionApi* | [**session**](docs/SessionApi.md#session) | **POST** /session | Initialize interactive session
*SwarmApi* | [**swarmInit**](docs/SwarmApi.md#swarminit) | **POST** /swarm/init | Initialize a new swarm
*SwarmApi* | [**swarmInspect**](docs/SwarmApi.md#swarminspect) | **GET** /swarm | Inspect swarm
*SwarmApi* | [**swarmJoin**](docs/SwarmApi.md#swarmjoin) | **POST** /swarm/join | Join an existing swarm
*SwarmApi* | [**swarmLeave**](docs/SwarmApi.md#swarmleave) | **POST** /swarm/leave | Leave a swarm
*SwarmApi* | [**swarmUnlock**](docs/SwarmApi.md#swarmunlock) | **POST** /swarm/unlock | Unlock a locked manager
*SwarmApi* | [**swarmUnlockkey**](docs/SwarmApi.md#swarmunlockkey) | **GET** /swarm/unlockkey | Get the unlock key
*SwarmApi* | [**swarmUpdate**](docs/SwarmApi.md#swarmupdate) | **POST** /swarm/update | Update a swarm
*SystemApi* | [**systemAuth**](docs/SystemApi.md#systemauth) | **POST** /auth | Check auth configuration
*SystemApi* | [**systemDataUsage**](docs/SystemApi.md#systemdatausage) | **GET** /system/df | Get data usage information
*SystemApi* | [**systemEvents**](docs/SystemApi.md#systemevents) | **GET** /events | Monitor events
*SystemApi* | [**systemInfo**](docs/SystemApi.md#systeminfo) | **GET** /info | Get system information
*SystemApi* | [**systemPing**](docs/SystemApi.md#systemping) | **GET** /_ping | Ping
*SystemApi* | [**systemPingHead**](docs/SystemApi.md#systempinghead) | **HEAD** /_ping | Ping
*SystemApi* | [**systemVersion**](docs/SystemApi.md#systemversion) | **GET** /version | Get version
*TaskApi* | [**taskInspect**](docs/TaskApi.md#taskinspect) | **GET** /tasks/{id} | Inspect a task
*TaskApi* | [**taskList**](docs/TaskApi.md#tasklist) | **GET** /tasks | List tasks
*TaskApi* | [**taskLogs**](docs/TaskApi.md#tasklogs) | **GET** /tasks/{id}/logs | Get task logs
*VolumeApi* | [**volumeCreate**](docs/VolumeApi.md#volumecreate) | **POST** /volumes/create | Create a volume
*VolumeApi* | [**volumeDelete**](docs/VolumeApi.md#volumedelete) | **DELETE** /volumes/{name} | Remove a volume
*VolumeApi* | [**volumeInspect**](docs/VolumeApi.md#volumeinspect) | **GET** /volumes/{name} | Inspect a volume
*VolumeApi* | [**volumeList**](docs/VolumeApi.md#volumelist) | **GET** /volumes | List volumes
*VolumeApi* | [**volumePrune**](docs/VolumeApi.md#volumeprune) | **POST** /volumes/prune | Delete unused volumes


<a name="documentation-for-models"></a>
## Documentation for Models

 - [de.gesellix.docker.engine.model.Address](docs/Address.md)
 - [de.gesellix.docker.engine.model.AuthConfig](docs/AuthConfig.md)
 - [de.gesellix.docker.engine.model.BuildCache](docs/BuildCache.md)
 - [de.gesellix.docker.engine.model.BuildInfo](docs/BuildInfo.md)
 - [de.gesellix.docker.engine.model.BuildPruneResponse](docs/BuildPruneResponse.md)
 - [de.gesellix.docker.engine.model.ClusterInfo](docs/ClusterInfo.md)
 - [de.gesellix.docker.engine.model.Commit](docs/Commit.md)
 - [de.gesellix.docker.engine.model.Config](docs/Config.md)
 - [de.gesellix.docker.engine.model.ConfigSpec](docs/ConfigSpec.md)
 - [de.gesellix.docker.engine.model.ContainerChangeResponseItem](docs/ContainerChangeResponseItem.md)
 - [de.gesellix.docker.engine.model.ContainerConfig](docs/ContainerConfig.md)
 - [de.gesellix.docker.engine.model.ContainerCreateResponse](docs/ContainerCreateResponse.md)
 - [de.gesellix.docker.engine.model.ContainerInspectResponse](docs/ContainerInspectResponse.md)
 - [de.gesellix.docker.engine.model.ContainerPruneResponse](docs/ContainerPruneResponse.md)
 - [de.gesellix.docker.engine.model.ContainerState](docs/ContainerState.md)
 - [de.gesellix.docker.engine.model.ContainerTopResponse](docs/ContainerTopResponse.md)
 - [de.gesellix.docker.engine.model.ContainerUpdateResponse](docs/ContainerUpdateResponse.md)
 - [de.gesellix.docker.engine.model.ContainerWaitResponse](docs/ContainerWaitResponse.md)
 - [de.gesellix.docker.engine.model.ContainerWaitResponseError](docs/ContainerWaitResponseError.md)
 - [de.gesellix.docker.engine.model.CreateImageInfo](docs/CreateImageInfo.md)
 - [de.gesellix.docker.engine.model.DeviceMapping](docs/DeviceMapping.md)
 - [de.gesellix.docker.engine.model.DeviceRequest](docs/DeviceRequest.md)
 - [de.gesellix.docker.engine.model.DistributionInspectResponse](docs/DistributionInspectResponse.md)
 - [de.gesellix.docker.engine.model.DistributionInspectResponseDescriptor](docs/DistributionInspectResponseDescriptor.md)
 - [de.gesellix.docker.engine.model.DistributionInspectResponsePlatforms](docs/DistributionInspectResponsePlatforms.md)
 - [de.gesellix.docker.engine.model.Driver](docs/Driver.md)
 - [de.gesellix.docker.engine.model.EndpointIPAMConfig](docs/EndpointIPAMConfig.md)
 - [de.gesellix.docker.engine.model.EndpointPortConfig](docs/EndpointPortConfig.md)
 - [de.gesellix.docker.engine.model.EndpointSettings](docs/EndpointSettings.md)
 - [de.gesellix.docker.engine.model.EndpointSpec](docs/EndpointSpec.md)
 - [de.gesellix.docker.engine.model.EngineDescription](docs/EngineDescription.md)
 - [de.gesellix.docker.engine.model.EngineDescriptionPlugins](docs/EngineDescriptionPlugins.md)
 - [de.gesellix.docker.engine.model.ErrorDetail](docs/ErrorDetail.md)
 - [de.gesellix.docker.engine.model.ErrorResponse](docs/ErrorResponse.md)
 - [de.gesellix.docker.engine.model.ExecInspectResponse](docs/ExecInspectResponse.md)
 - [de.gesellix.docker.engine.model.GraphDriverData](docs/GraphDriverData.md)
 - [de.gesellix.docker.engine.model.Health](docs/Health.md)
 - [de.gesellix.docker.engine.model.HealthConfig](docs/HealthConfig.md)
 - [de.gesellix.docker.engine.model.HealthcheckResult](docs/HealthcheckResult.md)
 - [de.gesellix.docker.engine.model.HistoryResponseItem](docs/HistoryResponseItem.md)
 - [de.gesellix.docker.engine.model.HostConfig](docs/HostConfig.md)
 - [de.gesellix.docker.engine.model.HostConfigAllOf](docs/HostConfigAllOf.md)
 - [de.gesellix.docker.engine.model.HostConfigAllOfLogConfig](docs/HostConfigAllOfLogConfig.md)
 - [de.gesellix.docker.engine.model.IPAM](docs/IPAM.md)
 - [de.gesellix.docker.engine.model.IdResponse](docs/IdResponse.md)
 - [de.gesellix.docker.engine.model.Image](docs/Image.md)
 - [de.gesellix.docker.engine.model.ImageDeleteResponseItem](docs/ImageDeleteResponseItem.md)
 - [de.gesellix.docker.engine.model.ImageID](docs/ImageID.md)
 - [de.gesellix.docker.engine.model.ImageMetadata](docs/ImageMetadata.md)
 - [de.gesellix.docker.engine.model.ImagePruneResponse](docs/ImagePruneResponse.md)
 - [de.gesellix.docker.engine.model.ImageRootFS](docs/ImageRootFS.md)
 - [de.gesellix.docker.engine.model.ImageSearchResponseItem](docs/ImageSearchResponseItem.md)
 - [de.gesellix.docker.engine.model.ImageSummary](docs/ImageSummary.md)
 - [de.gesellix.docker.engine.model.IndexInfo](docs/IndexInfo.md)
 - [de.gesellix.docker.engine.model.InlineObject](docs/InlineObject.md)
 - [de.gesellix.docker.engine.model.InlineObject1](docs/InlineObject1.md)
 - [de.gesellix.docker.engine.model.InlineObject2](docs/InlineObject2.md)
 - [de.gesellix.docker.engine.model.InlineObject3](docs/InlineObject3.md)
 - [de.gesellix.docker.engine.model.InlineObject4](docs/InlineObject4.md)
 - [de.gesellix.docker.engine.model.InlineObject5](docs/InlineObject5.md)
 - [de.gesellix.docker.engine.model.InlineObject6](docs/InlineObject6.md)
 - [de.gesellix.docker.engine.model.InlineObject7](docs/InlineObject7.md)
 - [de.gesellix.docker.engine.model.InlineResponse400](docs/InlineResponse400.md)
 - [de.gesellix.docker.engine.model.JoinTokens](docs/JoinTokens.md)
 - [de.gesellix.docker.engine.model.Limit](docs/Limit.md)
 - [de.gesellix.docker.engine.model.LocalNodeState](docs/LocalNodeState.md)
 - [de.gesellix.docker.engine.model.ManagerStatus](docs/ManagerStatus.md)
 - [de.gesellix.docker.engine.model.Mount](docs/Mount.md)
 - [de.gesellix.docker.engine.model.MountBindOptions](docs/MountBindOptions.md)
 - [de.gesellix.docker.engine.model.MountPoint](docs/MountPoint.md)
 - [de.gesellix.docker.engine.model.MountTmpfsOptions](docs/MountTmpfsOptions.md)
 - [de.gesellix.docker.engine.model.MountVolumeOptions](docs/MountVolumeOptions.md)
 - [de.gesellix.docker.engine.model.MountVolumeOptionsDriverConfig](docs/MountVolumeOptionsDriverConfig.md)
 - [de.gesellix.docker.engine.model.Network](docs/Network.md)
 - [de.gesellix.docker.engine.model.NetworkAttachmentConfig](docs/NetworkAttachmentConfig.md)
 - [de.gesellix.docker.engine.model.NetworkContainer](docs/NetworkContainer.md)
 - [de.gesellix.docker.engine.model.NetworkCreateResponse](docs/NetworkCreateResponse.md)
 - [de.gesellix.docker.engine.model.NetworkPruneResponse](docs/NetworkPruneResponse.md)
 - [de.gesellix.docker.engine.model.NetworkSettings](docs/NetworkSettings.md)
 - [de.gesellix.docker.engine.model.NetworkingConfig](docs/NetworkingConfig.md)
 - [de.gesellix.docker.engine.model.Node](docs/Node.md)
 - [de.gesellix.docker.engine.model.NodeDescription](docs/NodeDescription.md)
 - [de.gesellix.docker.engine.model.NodeSpec](docs/NodeSpec.md)
 - [de.gesellix.docker.engine.model.NodeState](docs/NodeState.md)
 - [de.gesellix.docker.engine.model.NodeStatus](docs/NodeStatus.md)
 - [de.gesellix.docker.engine.model.ObjectVersion](docs/ObjectVersion.md)
 - [de.gesellix.docker.engine.model.PeerNode](docs/PeerNode.md)
 - [de.gesellix.docker.engine.model.Platform](docs/Platform.md)
 - [de.gesellix.docker.engine.model.Plugin](docs/Plugin.md)
 - [de.gesellix.docker.engine.model.PluginConfig](docs/PluginConfig.md)
 - [de.gesellix.docker.engine.model.PluginConfigArgs](docs/PluginConfigArgs.md)
 - [de.gesellix.docker.engine.model.PluginConfigInterface](docs/PluginConfigInterface.md)
 - [de.gesellix.docker.engine.model.PluginConfigLinux](docs/PluginConfigLinux.md)
 - [de.gesellix.docker.engine.model.PluginConfigNetwork](docs/PluginConfigNetwork.md)
 - [de.gesellix.docker.engine.model.PluginConfigRootfs](docs/PluginConfigRootfs.md)
 - [de.gesellix.docker.engine.model.PluginConfigUser](docs/PluginConfigUser.md)
 - [de.gesellix.docker.engine.model.PluginDevice](docs/PluginDevice.md)
 - [de.gesellix.docker.engine.model.PluginEnv](docs/PluginEnv.md)
 - [de.gesellix.docker.engine.model.PluginInterfaceType](docs/PluginInterfaceType.md)
 - [de.gesellix.docker.engine.model.PluginMount](docs/PluginMount.md)
 - [de.gesellix.docker.engine.model.PluginPrivilegeItem](docs/PluginPrivilegeItem.md)
 - [de.gesellix.docker.engine.model.PluginSettings](docs/PluginSettings.md)
 - [de.gesellix.docker.engine.model.PluginsInfo](docs/PluginsInfo.md)
 - [de.gesellix.docker.engine.model.Port](docs/Port.md)
 - [de.gesellix.docker.engine.model.PortBinding](docs/PortBinding.md)
 - [de.gesellix.docker.engine.model.ProcessConfig](docs/ProcessConfig.md)
 - [de.gesellix.docker.engine.model.ProgressDetail](docs/ProgressDetail.md)
 - [de.gesellix.docker.engine.model.PushImageInfo](docs/PushImageInfo.md)
 - [de.gesellix.docker.engine.model.Reachability](docs/Reachability.md)
 - [de.gesellix.docker.engine.model.RegistryServiceConfig](docs/RegistryServiceConfig.md)
 - [de.gesellix.docker.engine.model.ResourceObject](docs/ResourceObject.md)
 - [de.gesellix.docker.engine.model.Resources](docs/Resources.md)
 - [de.gesellix.docker.engine.model.ResourcesBlkioWeightDevice](docs/ResourcesBlkioWeightDevice.md)
 - [de.gesellix.docker.engine.model.ResourcesUlimits](docs/ResourcesUlimits.md)
 - [de.gesellix.docker.engine.model.RestartPolicy](docs/RestartPolicy.md)
 - [de.gesellix.docker.engine.model.Runtime](docs/Runtime.md)
 - [de.gesellix.docker.engine.model.Secret](docs/Secret.md)
 - [de.gesellix.docker.engine.model.SecretSpec](docs/SecretSpec.md)
 - [de.gesellix.docker.engine.model.Service](docs/Service.md)
 - [de.gesellix.docker.engine.model.ServiceCreateResponse](docs/ServiceCreateResponse.md)
 - [de.gesellix.docker.engine.model.ServiceEndpoint](docs/ServiceEndpoint.md)
 - [de.gesellix.docker.engine.model.ServiceEndpointVirtualIPs](docs/ServiceEndpointVirtualIPs.md)
 - [de.gesellix.docker.engine.model.ServiceJobStatus](docs/ServiceJobStatus.md)
 - [de.gesellix.docker.engine.model.ServiceServiceStatus](docs/ServiceServiceStatus.md)
 - [de.gesellix.docker.engine.model.ServiceSpec](docs/ServiceSpec.md)
 - [de.gesellix.docker.engine.model.ServiceSpecMode](docs/ServiceSpecMode.md)
 - [de.gesellix.docker.engine.model.ServiceSpecModeReplicated](docs/ServiceSpecModeReplicated.md)
 - [de.gesellix.docker.engine.model.ServiceSpecModeReplicatedJob](docs/ServiceSpecModeReplicatedJob.md)
 - [de.gesellix.docker.engine.model.ServiceSpecRollbackConfig](docs/ServiceSpecRollbackConfig.md)
 - [de.gesellix.docker.engine.model.ServiceSpecUpdateConfig](docs/ServiceSpecUpdateConfig.md)
 - [de.gesellix.docker.engine.model.ServiceUpdateResponse](docs/ServiceUpdateResponse.md)
 - [de.gesellix.docker.engine.model.ServiceUpdateStatus](docs/ServiceUpdateStatus.md)
 - [de.gesellix.docker.engine.model.Swarm](docs/Swarm.md)
 - [de.gesellix.docker.engine.model.SwarmAllOf](docs/SwarmAllOf.md)
 - [de.gesellix.docker.engine.model.SwarmInfo](docs/SwarmInfo.md)
 - [de.gesellix.docker.engine.model.SwarmSpec](docs/SwarmSpec.md)
 - [de.gesellix.docker.engine.model.SwarmSpecCAConfig](docs/SwarmSpecCAConfig.md)
 - [de.gesellix.docker.engine.model.SwarmSpecCAConfigExternalCAs](docs/SwarmSpecCAConfigExternalCAs.md)
 - [de.gesellix.docker.engine.model.SwarmSpecDispatcher](docs/SwarmSpecDispatcher.md)
 - [de.gesellix.docker.engine.model.SwarmSpecEncryptionConfig](docs/SwarmSpecEncryptionConfig.md)
 - [de.gesellix.docker.engine.model.SwarmSpecOrchestration](docs/SwarmSpecOrchestration.md)
 - [de.gesellix.docker.engine.model.SwarmSpecRaft](docs/SwarmSpecRaft.md)
 - [de.gesellix.docker.engine.model.SwarmSpecTaskDefaults](docs/SwarmSpecTaskDefaults.md)
 - [de.gesellix.docker.engine.model.SwarmSpecTaskDefaultsLogDriver](docs/SwarmSpecTaskDefaultsLogDriver.md)
 - [de.gesellix.docker.engine.model.SystemAuthResponse](docs/SystemAuthResponse.md)
 - [de.gesellix.docker.engine.model.SystemDataUsageResponse](docs/SystemDataUsageResponse.md)
 - [de.gesellix.docker.engine.model.SystemEventsResponse](docs/SystemEventsResponse.md)
 - [de.gesellix.docker.engine.model.SystemEventsResponseActor](docs/SystemEventsResponseActor.md)
 - [de.gesellix.docker.engine.model.SystemInfo](docs/SystemInfo.md)
 - [de.gesellix.docker.engine.model.SystemInfoDefaultAddressPools](docs/SystemInfoDefaultAddressPools.md)
 - [de.gesellix.docker.engine.model.SystemVersion](docs/SystemVersion.md)
 - [de.gesellix.docker.engine.model.SystemVersionComponents](docs/SystemVersionComponents.md)
 - [de.gesellix.docker.engine.model.SystemVersionPlatform](docs/SystemVersionPlatform.md)
 - [de.gesellix.docker.engine.model.TLSInfo](docs/TLSInfo.md)
 - [de.gesellix.docker.engine.model.Task](docs/Task.md)
 - [de.gesellix.docker.engine.model.TaskSpec](docs/TaskSpec.md)
 - [de.gesellix.docker.engine.model.TaskSpecContainerSpec](docs/TaskSpecContainerSpec.md)
 - [de.gesellix.docker.engine.model.TaskSpecContainerSpecConfigs](docs/TaskSpecContainerSpecConfigs.md)
 - [de.gesellix.docker.engine.model.TaskSpecContainerSpecDNSConfig](docs/TaskSpecContainerSpecDNSConfig.md)
 - [de.gesellix.docker.engine.model.TaskSpecContainerSpecFile](docs/TaskSpecContainerSpecFile.md)
 - [de.gesellix.docker.engine.model.TaskSpecContainerSpecFile1](docs/TaskSpecContainerSpecFile1.md)
 - [de.gesellix.docker.engine.model.TaskSpecContainerSpecPrivileges](docs/TaskSpecContainerSpecPrivileges.md)
 - [de.gesellix.docker.engine.model.TaskSpecContainerSpecPrivilegesCredentialSpec](docs/TaskSpecContainerSpecPrivilegesCredentialSpec.md)
 - [de.gesellix.docker.engine.model.TaskSpecContainerSpecPrivilegesSELinuxContext](docs/TaskSpecContainerSpecPrivilegesSELinuxContext.md)
 - [de.gesellix.docker.engine.model.TaskSpecContainerSpecSecrets](docs/TaskSpecContainerSpecSecrets.md)
 - [de.gesellix.docker.engine.model.TaskSpecLogDriver](docs/TaskSpecLogDriver.md)
 - [de.gesellix.docker.engine.model.TaskSpecNetworkAttachmentSpec](docs/TaskSpecNetworkAttachmentSpec.md)
 - [de.gesellix.docker.engine.model.TaskSpecPlacement](docs/TaskSpecPlacement.md)
 - [de.gesellix.docker.engine.model.TaskSpecPlacementPreferences](docs/TaskSpecPlacementPreferences.md)
 - [de.gesellix.docker.engine.model.TaskSpecPlacementSpread](docs/TaskSpecPlacementSpread.md)
 - [de.gesellix.docker.engine.model.TaskSpecPluginSpec](docs/TaskSpecPluginSpec.md)
 - [de.gesellix.docker.engine.model.TaskSpecResources](docs/TaskSpecResources.md)
 - [de.gesellix.docker.engine.model.TaskSpecRestartPolicy](docs/TaskSpecRestartPolicy.md)
 - [de.gesellix.docker.engine.model.TaskState](docs/TaskState.md)
 - [de.gesellix.docker.engine.model.TaskStatus](docs/TaskStatus.md)
 - [de.gesellix.docker.engine.model.TaskStatusContainerStatus](docs/TaskStatusContainerStatus.md)
 - [de.gesellix.docker.engine.model.ThrottleDevice](docs/ThrottleDevice.md)
 - [de.gesellix.docker.engine.model.UnlockKeyResponse](docs/UnlockKeyResponse.md)
 - [de.gesellix.docker.engine.model.Volume](docs/Volume.md)
 - [de.gesellix.docker.engine.model.VolumeConfig](docs/VolumeConfig.md)
 - [de.gesellix.docker.engine.model.VolumeListResponse](docs/VolumeListResponse.md)
 - [de.gesellix.docker.engine.model.VolumePruneResponse](docs/VolumePruneResponse.md)
 - [de.gesellix.docker.engine.model.VolumeUsageData](docs/VolumeUsageData.md)


<a name="documentation-for-authorization"></a>
## Documentation for Authorization

All endpoints do not require authorization.
