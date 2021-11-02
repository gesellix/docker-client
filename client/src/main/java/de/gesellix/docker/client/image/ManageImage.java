package de.gesellix.docker.client.image;

import de.gesellix.docker.client.DockerAsyncCallback;
import de.gesellix.docker.client.Timeout;
import de.gesellix.docker.engine.EngineResponse;

import java.io.InputStream;
import java.util.Map;

public interface ManageImage {

  BuildResult buildWithLogs(InputStream buildContext);

  BuildResult buildWithLogs(InputStream buildContext, BuildConfig config);

  /**
   * @see #buildWithLogs(InputStream, BuildConfig)
   * @deprecated use buildWithLogs(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
   */
  @Deprecated
  Object buildWithLogs(InputStream buildContext, Map<String, Object> query);

  /**
   * @see #buildWithLogs(InputStream, BuildConfig)
   * @deprecated use buildWithLogs(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
   */
  @Deprecated
  Object buildWithLogs(InputStream buildContext, Map<String, Object> query, Timeout timeout);

  BuildResult build(InputStream buildContext);

  BuildResult build(InputStream buildContext, BuildConfig config);

  /**
   * @see #build(InputStream, BuildConfig)
   * @deprecated use build(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
   */
  @Deprecated
  Object build(InputStream buildContext, Map<String, Object> query);

  /**
   * @see #build(InputStream, BuildConfig)
   * @deprecated use build(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
   */
  @Deprecated
  Object build(InputStream buildContext, Map<String, Object> query, DockerAsyncCallback callback);

  EngineResponse history(String image);

  Object importUrl(String url);

  Object importUrl(String url, String repository);

  Object importUrl(String url, String repository, String tag);

  String importStream(InputStream stream);

  String importStream(InputStream stream, String repository);

  String importStream(InputStream stream, String repository, String tag);

  EngineResponse inspectImage(String image);

  EngineResponse load(InputStream stream);

  EngineResponse images();

  EngineResponse images(Map<String, Object> query);

  EngineResponse pruneImages();

  EngineResponse pruneImages(Map<String, Object> query);

  EngineResponse create(Map<String, Object> query);

  EngineResponse create(Map<String, Object> query, Map<String, Object> createOptions);

  /**
   * @see #create(Map, Map)
   * @deprecated please use #create(query, createOptions)
   */
  @Deprecated
  String pull(String image);

  /**
   * @see #create(Map, Map)
   * @deprecated please use #create(query, createOptions)
   */
  @Deprecated
  String pull(String image, String tag);

  /**
   * @see #create(Map, Map)
   * @deprecated please use #create(query, createOptions)
   */
  @Deprecated
  String pull(String image, String tag, String authBase64Encoded);

  /**
   * @see #create(Map, Map)
   * @deprecated please use #create(query, createOptions)
   */
  @Deprecated
  String pull(String image, String tag, String authBase64Encoded, String registry);

  EngineResponse push(String image);

  EngineResponse push(String image, String authBase64Encoded);

  EngineResponse push(String image, String authBase64Encoded, String registry);

  EngineResponse rmi(String image);

  EngineResponse save(String[] images);

  EngineResponse tag(String image, String repository);

  String findImageId(String imageName);

  String findImageId(String imageName, String tag);
}
