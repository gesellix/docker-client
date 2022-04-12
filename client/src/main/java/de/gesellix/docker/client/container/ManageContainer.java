package de.gesellix.docker.client.container;

import de.gesellix.docker.client.EngineResponseContent;
import de.gesellix.docker.engine.AttachConfig;
import de.gesellix.docker.engine.EngineResponse;
import de.gesellix.docker.remote.api.ContainerChangeResponseItem;
import de.gesellix.docker.remote.api.ContainerCreateRequest;
import de.gesellix.docker.remote.api.ContainerCreateResponse;
import de.gesellix.docker.remote.api.ContainerInspectResponse;
import de.gesellix.docker.remote.api.ContainerPruneResponse;
import de.gesellix.docker.remote.api.ContainerTopResponse;
import de.gesellix.docker.remote.api.ContainerUpdateRequest;
import de.gesellix.docker.remote.api.ContainerUpdateResponse;
import de.gesellix.docker.remote.api.ContainerWaitResponse;
import de.gesellix.docker.remote.api.ExecConfig;
import de.gesellix.docker.remote.api.ExecInspectResponse;
import de.gesellix.docker.remote.api.ExecStartConfig;
import de.gesellix.docker.remote.api.IdResponse;
import de.gesellix.docker.remote.api.core.Frame;
import de.gesellix.docker.remote.api.core.StreamCallback;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface ManageContainer {

  EngineResponse attach(String container, Map<String, Object> query);

  EngineResponse attach(String container, Map<String, Object> query, AttachConfig callback);

  void attach(String containerId,
              String detachKeys,
              Boolean logs, Boolean stream,
              Boolean stdin, Boolean stdout, Boolean stderr,
              StreamCallback<Frame> callback, Duration timeout);

  void resizeTTY(String container, Integer height, Integer width);

  WebSocket attachWebsocket(String container, Map<String, Object> query, WebSocketListener listener);

  EngineResponseContent<IdResponse> commit(String container, Map query);

  EngineResponseContent<IdResponse> commit(String container, Map query, Map config);

  EngineResponseContent<Map<String, Object>> getArchiveStats(String container, String path);

  byte[] extractFile(String container, String filename);

  EngineResponseContent<InputStream> getArchive(String container, String path);

  void putArchive(String container, String path, InputStream archive);

  EngineResponseContent<ContainerCreateResponse> createContainer(ContainerCreateRequest containerCreateRequest);

  EngineResponseContent<ContainerCreateResponse> createContainer(ContainerCreateRequest containerCreateRequest, String name);

  EngineResponseContent<ContainerCreateResponse> createContainer(ContainerCreateRequest containerCreateRequest, String name, String authBase64Encoded);

  EngineResponseContent<List<ContainerChangeResponseItem>> diff(String container);

  EngineResponseContent<IdResponse> createExec(String container, ExecConfig execConfig);

  void startExec(String execId, ExecStartConfig execStartConfig, AttachConfig attachConfig);

  void startExec(String execId, ExecStartConfig execStartConfig, StreamCallback<Frame> callback, Duration timeout);

  EngineResponseContent<ExecInspectResponse> inspectExec(String execId);

  EngineResponseContent<IdResponse> exec(String container, List<String> command, StreamCallback<Frame> callback, Duration timeout);

  EngineResponseContent<IdResponse> exec(String container, List<String> command, StreamCallback<Frame> callback, Duration timeout, Map<String, Object> execConfig);

  void resizeExec(String exec, Integer height, Integer width);

  EngineResponseContent<InputStream> export(String container);

  EngineResponseContent<ContainerInspectResponse> inspectContainer(String container);

  void kill(String container);

  void logs(String container, Map<String, Object> query, StreamCallback<Frame> callback, Duration timeout);

  EngineResponseContent<List<Map<String, Object>>> ps(Map<String, Object> query);

  EngineResponseContent<List<Map<String, Object>>> ps();

  EngineResponseContent<List<Map<String, Object>>> ps(Boolean all);

  EngineResponseContent<List<Map<String, Object>>> ps(Boolean all, Integer limit);

  EngineResponseContent<List<Map<String, Object>>> ps(Boolean all, Integer limit, Boolean size);

  EngineResponseContent<List<Map<String, Object>>> ps(Boolean all, Integer limit, Boolean size, String filters);

  void pause(String container);

  EngineResponseContent<ContainerPruneResponse> pruneContainers();

  EngineResponseContent<ContainerPruneResponse> pruneContainers(String filters);

  void rename(String container, String newName);

  void restart(String containerIdOrName);

  void rm(String containerIdOrName);

  void rm(String containerIdOrName, Map<String, Object> query);

  EngineResponseContent<ContainerCreateResponse> run(ContainerCreateRequest containerCreateRequest);

  EngineResponseContent<ContainerCreateResponse> run(ContainerCreateRequest containerCreateRequest, String name);

  EngineResponseContent<ContainerCreateResponse> run(ContainerCreateRequest containerCreateRequest, String name, String authBase64Encoded);

  void startContainer(String container);

  void stats(String container, Boolean stream, StreamCallback<Object> callback, Duration timeout);

  void stop(String containerIdOrName);

  void stop(String containerIdOrName, Integer timeoutSeconds);

  void stop(String containerIdOrName, Duration timeout);

  EngineResponseContent<ContainerTopResponse> top(String containerIdOrName);

  EngineResponseContent<ContainerTopResponse> top(String containerIdOrName, String psArgs);

  void unpause(String container);

  EngineResponseContent<ContainerUpdateResponse> updateContainer(String container, ContainerUpdateRequest containerUpdateRequest);

  EngineResponseContent<ContainerWaitResponse> wait(String containerIdOrName);
}
