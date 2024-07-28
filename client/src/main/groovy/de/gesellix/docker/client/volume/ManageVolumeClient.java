package de.gesellix.docker.client.volume;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.ClusterVolumeSpec;
import de.gesellix.docker.remote.api.EngineApiClient;
import de.gesellix.docker.remote.api.Volume;
import de.gesellix.docker.remote.api.VolumeCreateOptions;
import de.gesellix.docker.remote.api.VolumeListResponse;
import de.gesellix.docker.remote.api.VolumePruneResponse;
import de.gesellix.util.QueryParameterEncoder;

public class ManageVolumeClient implements ManageVolume {

  private final Logger log = LoggerFactory.getLogger(ManageVolumeClient.class);
  private final EngineApiClient client;
  private final QueryParameterEncoder queryParameterEncoder;

  public ManageVolumeClient(EngineApiClient client) {
    this.client = client;
    this.queryParameterEncoder = new QueryParameterEncoder();
  }

  /**
   * @see #volumes(String)
   * @deprecated use {@link #volumes(String)}
   */
  @Deprecated
  @Override
  public EngineResponseContent<VolumeListResponse> volumes(Map<String, Object> query) {
    log.info("docker volume ls");
    Map<String, Object> actualQuery = new HashMap<>();
    if (query != null) {
      actualQuery.putAll(query);
    }

    queryParameterEncoder.jsonEncodeQueryParameter(actualQuery, "filters");
    return volumes((String) actualQuery.get("filters"));
  }

  @Override
  public EngineResponseContent<VolumeListResponse> volumes(String filters) {
    log.info("docker volume ls");
    VolumeListResponse volumeList = client.getVolumeApi().volumeList(filters);
    return new EngineResponseContent<>(volumeList);
  }

  @Override
  public EngineResponseContent<VolumeListResponse> volumes() {
    return volumes((String) null);
  }

  @Override
  public EngineResponseContent<Volume> inspectVolume(String name) {
    log.info("docker volume inspect");
    Volume volumeInspect = client.getVolumeApi().volumeInspect(name);
    return new EngineResponseContent<>(volumeInspect);
  }

  /**
   * @see #createVolume(VolumeCreateOptions)
   * @deprecated use {@link #createVolume(VolumeCreateOptions)}
   */
  @Deprecated
  @Override
  public EngineResponseContent<Volume> createVolume(Map<String, Object> config) {
    log.info("docker volume create");
    return createVolume(new VolumeCreateOptions(
        config == null ? null : (String) config.get("Name"),
        config == null ? null : (String) config.get("Driver"),
        config == null ? null : (Map) config.get("DriverOpts"),
        config == null ? null : (Map) config.get("Labels"),
        config == null ? null : (ClusterVolumeSpec) config.get("ClusterVolumeSpec")));
  }

  @Override
  public EngineResponseContent<Volume> createVolume(VolumeCreateOptions volumeConfig) {
    log.info("docker volume create");
    Volume volume = client.getVolumeApi().volumeCreate(volumeConfig);
    return new EngineResponseContent<>(volume);
  }

  @Override
  public EngineResponseContent<Volume> createVolume() {
    return createVolume(new VolumeCreateOptions());
  }

  @Override
  public void rmVolume(String name) {
    log.info("docker volume rm");
    client.getVolumeApi().volumeDelete(name, null);
  }

  /**
   * @see #pruneVolumes(String)
   * @deprecated use {@link #pruneVolumes(String)}
   */
  @Deprecated
  @Override
  public EngineResponseContent<VolumePruneResponse> pruneVolumes(Map<String, Object> query) {
    log.info("docker volume prune");
    Map<String, Object> actualQuery = new HashMap<>();
    if (query != null) {
      actualQuery.putAll(query);
    }

    queryParameterEncoder.jsonEncodeQueryParameter(actualQuery, "filters");
    return pruneVolumes((String) actualQuery.get("filters"));
  }

  @Override
  public EngineResponseContent<VolumePruneResponse> pruneVolumes(String filters) {
    log.info("docker volume prune");
    VolumePruneResponse pruneResponse = client.getVolumeApi().volumePrune(filters);
    return new EngineResponseContent<>(pruneResponse);
  }

  @Override
  public EngineResponseContent<VolumePruneResponse> pruneVolumes() {
    return pruneVolumes((String) null);
  }
}
