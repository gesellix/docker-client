package de.gesellix.docker.client.image;

import de.gesellix.docker.engine.EngineResponse;
import de.gesellix.docker.remote.api.BuildInfo;
import de.gesellix.docker.remote.api.CreateImageInfo;
import de.gesellix.docker.remote.api.HistoryResponseItem;
import de.gesellix.docker.remote.api.Image;
import de.gesellix.docker.remote.api.ImageDeleteResponseItem;
import de.gesellix.docker.remote.api.ImagePruneResponse;
import de.gesellix.docker.remote.api.ImageSearchResponseItem;
import de.gesellix.docker.remote.api.ImageSummary;
import de.gesellix.docker.remote.api.PushImageInfo;
import de.gesellix.docker.remote.api.core.StreamCallback;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface ManageImage {

  EngineResponse<List<ImageSearchResponseItem>> search(String term);

  EngineResponse<List<ImageSearchResponseItem>> search(String term, Integer limit);

  void build(InputStream buildContext);

  void build(StreamCallback<BuildInfo> callback, Duration timeout,
             InputStream buildContext);

  void build(StreamCallback<BuildInfo> callback, Duration timeout,
             String tag,
             InputStream buildContext);

  void build(String tag,
             InputStream buildContext);

  void build(String dockerfile, String tag, Boolean quiet, Boolean nocache, String pull, Boolean rm,
             String buildargs, String labels, String encodedRegistryConfig, String contentType,
             InputStream buildContext);

  void build(StreamCallback<BuildInfo> callback, Duration timeout,
             String dockerfile, String tag, Boolean quiet, Boolean nocache, String pull, Boolean rm,
             String buildargs, String labels, String encodedRegistryConfig, String contentType,
             InputStream buildContext);

  EngineResponse<List<HistoryResponseItem>> history(String image);

  EngineResponse<Image> inspectImage(String image);

  void load(InputStream imagesTarball);

  /**
   * @see #images(Boolean, String, Boolean)
   * @deprecated use {@link #images(Boolean, String, Boolean)}
   */
  @Deprecated
  EngineResponse<List<ImageSummary>> images(Map<String, Object> query);

  EngineResponse<List<ImageSummary>> images();

  EngineResponse<List<ImageSummary>> images(Boolean all, String filters, Boolean digests);

  /**
   * @see #pruneImages(String)
   * @deprecated use {@link #pruneImages(String)}
   */
  @Deprecated
  EngineResponse<ImagePruneResponse> pruneImages(Map<String, Object> query);

  EngineResponse<ImagePruneResponse> pruneImages();

  EngineResponse<ImagePruneResponse> pruneImages(String filters);

  void pull(StreamCallback<CreateImageInfo> callback, Duration timeout,
            String imageName);

  void pull(StreamCallback<CreateImageInfo> callback, Duration timeout,
            String imageName, String tag);

  void pull(StreamCallback<CreateImageInfo> callback, Duration timeout,
            String imageName, String tag, String authBase64Encoded);

  void importUrl(StreamCallback<CreateImageInfo> callback, Duration timeout,
                 String url);

  void importUrl(StreamCallback<CreateImageInfo> callback, Duration timeout,
                 String url, String repository);

  void importUrl(StreamCallback<CreateImageInfo> callback, Duration timeout,
                 String url, String repository, String tag);

  void importStream(StreamCallback<CreateImageInfo> callback, Duration timeout,
                    InputStream stream);

  void importStream(StreamCallback<CreateImageInfo> callback, Duration timeout,
                    InputStream stream, String repository);

  void importStream(StreamCallback<CreateImageInfo> callback, Duration timeout,
                    InputStream stream, String repository, String tag);

  void push(String image);

  void push(StreamCallback<PushImageInfo> callback, Duration timeout, String image);

  void push(String image, String authBase64Encoded);

  void push(StreamCallback<PushImageInfo> callback, Duration timeout, String image, String authBase64Encoded);

  void push(String image, String authBase64Encoded, String registry);

  void push(StreamCallback<PushImageInfo> callback, Duration timeout, String image, String authBase64Encoded, String registry);

  EngineResponse<List<ImageDeleteResponseItem>> rmi(String image);

  EngineResponse<InputStream> save(List<String> images);

  void tag(String image, String repository);

  String findImageId(String imageName);

  String findImageId(String imageName, String tag);
}
