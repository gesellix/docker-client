package de.gesellix.docker.client.container;

import de.gesellix.docker.client.DockerAsyncCallback;
import de.gesellix.docker.engine.AttachConfig;
import de.gesellix.docker.engine.EngineResponse;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface ManageContainer {

  EngineResponse attach(String container, Map<String, Object> query);

  EngineResponse attach(String container, Map<String, Object> query, AttachConfig callback);

  EngineResponse resizeTTY(String container, Integer height, Integer width);

  WebSocket attachWebsocket(String container, Map<String, Object> query, WebSocketListener listener);

  EngineResponse commit(String container, Map query);

  EngineResponse commit(String container, Map query, Map config);

  Object getArchiveStats(String container, String path);

  byte[] extractFile(String container, String filename);

  EngineResponse getArchive(String container, String path);

  EngineResponse putArchive(String container, String path, InputStream archive);

  EngineResponse putArchive(String container, String path, InputStream archive, Map<String, ?> query);

  EngineResponse createContainer(Map<String, Object> containerConfig);

  EngineResponse createContainer(Map<String, Object> containerConfig, Map<String, Object> query);

  EngineResponse createContainer(Map<String, Object> containerConfig, Map<String, Object> query, String authBase64Encoded);

  EngineResponse diff(String container);

  EngineResponse createExec(String container, Map execConfig);

  EngineResponse startExec(String execId, Map execConfig);

  EngineResponse startExec(String execId, Map execConfig, AttachConfig attachConfig);

  EngineResponse inspectExec(String execId);

  EngineResponse exec(String container, List<String> command);

  EngineResponse exec(String container, List<String> command, Map<String, Object> execConfig);

  EngineResponse resizeExec(String exec, Integer height, Integer width);

  EngineResponse export(String container);

  EngineResponse inspectContainer(String container);

  EngineResponse kill(String container);

  EngineResponse logs(String container);

  EngineResponse logs(String container, DockerAsyncCallback callback);

  EngineResponse logs(String container, Map<String, Object> query);

  EngineResponse logs(String container, Map<String, Object> query, DockerAsyncCallback callback);

  EngineResponse ps();

  EngineResponse ps(Map<String, Object> query);

  EngineResponse pause(String container);

  EngineResponse pruneContainers();

  EngineResponse pruneContainers(Object query);

  EngineResponse rename(String container, String newName);

  EngineResponse restart(String containerIdOrName);

  EngineResponse rm(String containerIdOrName);

  EngineResponse rm(String containerIdOrName, Map<String, Object> query);

  Object run(String image, Map<String, Object> containerConfig);

  Object run(String image, Map<String, Object> containerConfig, String tag);

  Object run(String image, Map<String, Object> containerConfig, String tag, String name);

  Object run(String image, Map<String, Object> containerConfig, String tag, String name, String authBase64Encoded);

  EngineResponse startContainer(String container);

  EngineResponse stats(String container);

  EngineResponse stats(String container, DockerAsyncCallback callback);

  EngineResponse stop(String containerIdOrName);

  EngineResponse stop(String containerIdOrName, Integer timeout);

  EngineResponse top(String containerIdOrName);

  EngineResponse top(String containerIdOrName, String ps_args);

  EngineResponse unpause(String container);

  EngineResponse updateContainer(String container, Map<String, Object> containerConfig);

  Map<String, EngineResponse> updateContainers(List<String> containers, Map<String, Object> containerConfig);

  EngineResponse wait(String containerIdOrName);
}
