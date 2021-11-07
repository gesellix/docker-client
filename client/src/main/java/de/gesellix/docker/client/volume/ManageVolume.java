package de.gesellix.docker.client.volume;

import de.gesellix.docker.engine.EngineResponse;
import de.gesellix.docker.remote.api.Volume;
import de.gesellix.docker.remote.api.VolumeConfig;
import de.gesellix.docker.remote.api.VolumeListResponse;
import de.gesellix.docker.remote.api.VolumePruneResponse;

import java.util.Map;

public interface ManageVolume {

  /**
   * @see #createVolume(VolumeConfig)
   * @deprecated use {@link #createVolume(VolumeConfig)}
   */
  @Deprecated
  EngineResponse<Volume> createVolume(Map<String, Object> config);

  EngineResponse<Volume> createVolume();

  EngineResponse<Volume> createVolume(VolumeConfig volumeConfig);

  EngineResponse<Volume> inspectVolume(String name);

  /**
   * @see #volumes(String)
   * @deprecated use {@link #volumes(String)}
   */
  @Deprecated
  EngineResponse<VolumeListResponse> volumes(Map<String, Object> query);

  EngineResponse<VolumeListResponse> volumes();

  EngineResponse<VolumeListResponse> volumes(String filters);

  /**
   * @see #pruneVolumes(String)
   * @deprecated use {@link #pruneVolumes(String)}
   */
  @Deprecated
  EngineResponse<VolumePruneResponse> pruneVolumes(Map<String, Object> query);

  EngineResponse<VolumePruneResponse> pruneVolumes();

  EngineResponse<VolumePruneResponse> pruneVolumes(String filters);

  void rmVolume(String name);
}
