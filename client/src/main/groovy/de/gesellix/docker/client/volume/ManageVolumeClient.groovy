package de.gesellix.docker.client.volume

import de.gesellix.docker.client.EngineResponseContent
import de.gesellix.docker.remote.api.EngineApiClient
import de.gesellix.docker.remote.api.Volume
import de.gesellix.docker.remote.api.VolumeCreateOptions
import de.gesellix.docker.remote.api.VolumeListResponse
import de.gesellix.docker.remote.api.VolumePruneResponse
import de.gesellix.util.QueryUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ManageVolumeClient implements ManageVolume {

  private final Logger log = LoggerFactory.getLogger(ManageVolumeClient)

  private EngineApiClient client
  private QueryUtil queryUtil

  ManageVolumeClient(EngineApiClient client) {
    this.client = client
    this.queryUtil = new QueryUtil()
  }

  @Override
  EngineResponseContent<VolumeListResponse> volumes(Map<String, Object> query) {
    log.info("docker volume ls")
    Map<String, Object> actualQuery = new HashMap<String, Object>()
    if (query) {
      actualQuery.putAll(query)
    }
    queryUtil.jsonEncodeQueryParameter(actualQuery, "filters")
    return volumes(actualQuery.filters as String)
  }

  @Override
  EngineResponseContent<VolumeListResponse> volumes(String filters = null) {
    log.info("docker volume ls")
    VolumeListResponse volumeList = client.getVolumeApi().volumeList(filters)
    return new EngineResponseContent(volumeList)
  }

  @Override
  EngineResponseContent<Volume> inspectVolume(String name) {
    log.info("docker volume inspect")
    Volume volumeInspect = client.getVolumeApi().volumeInspect(name)
    return new EngineResponseContent(volumeInspect)
  }

  @Override
  EngineResponseContent<Volume> createVolume(Map<String, Object> config) {
    log.info("docker volume create")
    return createVolume(new VolumeCreateOptions(config?.Name as String, config?.Driver as String, config?.DriverOpts as Map, config?.Labels as Map))
  }

  @Override
  EngineResponseContent<Volume> createVolume(VolumeCreateOptions volumeConfig = new VolumeCreateOptions()) {
    log.info("docker volume create")
    Volume volume = client.getVolumeApi().volumeCreate(volumeConfig)
    return new EngineResponseContent<Volume>(volume)
  }

  @Override
  void rmVolume(String name) {
    log.info("docker volume rm")
    client.getVolumeApi().volumeDelete(name, null)
  }

  @Override
  EngineResponseContent<VolumePruneResponse> pruneVolumes(Map<String, Object> query) {
    log.info("docker volume prune")
    Map<String, Object> actualQuery = new HashMap<String, Object>()
    if (query) {
      actualQuery.putAll(query)
    }
    queryUtil.jsonEncodeQueryParameter(actualQuery, "filters")
    return pruneVolumes(actualQuery.filters as String)
  }

  @Override
  EngineResponseContent<VolumePruneResponse> pruneVolumes(String filters = null) {
    log.info("docker volume prune")
    VolumePruneResponse pruneResponse = client.getVolumeApi().volumePrune(filters)
    return new EngineResponseContent<VolumePruneResponse>(pruneResponse)
  }
}
