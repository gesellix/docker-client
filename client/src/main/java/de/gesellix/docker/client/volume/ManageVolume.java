package de.gesellix.docker.client.volume;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.remote.api.Volume;
import de.gesellix.docker.remote.api.VolumeCreateOptions;
import de.gesellix.docker.remote.api.VolumeListResponse;
import de.gesellix.docker.remote.api.VolumePruneResponse;

import java.util.Map;

public interface ManageVolume {

  EngineResponseContent<Volume> createVolume();

  EngineResponseContent<Volume> createVolume(VolumeCreateOptions volumeCreateOptions);

  EngineResponseContent<Volume> inspectVolume(String name);

  /**
   * @see #volumes(String)
   * @deprecated use {@link #volumes(String)}
   */
  @Deprecated
  EngineResponseContent<VolumeListResponse> volumes(Map<String, Object> query);

  EngineResponseContent<VolumeListResponse> volumes();

  EngineResponseContent<VolumeListResponse> volumes(String filters);

  /**
   * @see #pruneVolumes(String)
   * @deprecated use {@link #pruneVolumes(String)}
   */
  @Deprecated
  EngineResponseContent<VolumePruneResponse> pruneVolumes(Map<String, Object> query);

  EngineResponseContent<VolumePruneResponse> pruneVolumes();

  EngineResponseContent<VolumePruneResponse> pruneVolumes(String filters);

  void rmVolume(String name);
}
