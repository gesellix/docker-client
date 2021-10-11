package de.gesellix.docker.client;

import com.squareup.moshi.Moshi;
import de.gesellix.docker.builder.BuildContextBuilder;
import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.DockerRegistry;
import de.gesellix.docker.client.testutil.HttpTestServer;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.client.testutil.ManifestUtil;
import de.gesellix.docker.client.testutil.NetworkInterfaces;
import de.gesellix.docker.client.testutil.TarUtil;
import de.gesellix.docker.client.testutil.TestImage;
import de.gesellix.docker.remote.api.BuildPruneResponse;
import de.gesellix.docker.remote.api.ContainerCreateRequest;
import de.gesellix.docker.remote.api.ContainerCreateResponse;
import de.gesellix.docker.remote.api.HistoryResponseItem;
import de.gesellix.docker.remote.api.IdResponse;
import de.gesellix.docker.remote.api.Image;
import de.gesellix.docker.remote.api.ImageDeleteResponseItem;
import de.gesellix.docker.remote.api.ImageSearchResponseItem;
import de.gesellix.docker.remote.api.ImageSummary;
import de.gesellix.docker.remote.api.client.ContainerApi;
import de.gesellix.docker.remote.api.client.ImageApi;
import de.gesellix.testutil.ResourceReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.gesellix.docker.client.testutil.Constants.LABEL_KEY;
import static de.gesellix.docker.client.testutil.Constants.LABEL_VALUE;
import static java.nio.file.Files.readAttributes;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DockerEngineAvailable
class ImageApiIntegrationTest {

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  private TestImage testImage;

  ImageApi imageApi;
  ContainerApi containerApi;

  @BeforeEach
  public void setup() {
    imageApi = typeSafeDockerClient.getImageApi();
    containerApi = typeSafeDockerClient.getContainerApi();
    testImage = new TestImage(typeSafeDockerClient);
  }

  @Test
  public void buildPrune() {
    BuildPruneResponse response = imageApi.buildPrune(null, null, null);
    assertTrue(response.getSpaceReclaimed() >= 0);
  }

  @Test
  public void imageBuildAndPrune() throws IOException {
    imageApi.imageTag(testImage.getImageWithTag(), "test", "build-base");
    String dockerfile = "/images/builder/Dockerfile";
    File inputDirectory = ResourceReader.getClasspathResourceAsFile(dockerfile, ImageApi.class).getParentFile();
    InputStream buildContext = newBuildContext(inputDirectory);

    assertDoesNotThrow(() -> imageApi.imageBuild(Paths.get(dockerfile).getFileName().toString(), "test:build", null, null, null, null, null, null,
                                                 null, null, null, null, null, null, null,
                                                 null, null, null, null, null, null, null,
                                                 null, null, null, null, buildContext));

    Map<String, List<String>> filter = new HashMap<>();
    filter.put("label", singletonList(LABEL_KEY));
    String filterJson = new Moshi.Builder().build().adapter(Map.class).toJson(filter);
    assertDoesNotThrow(() -> imageApi.imagePrune(filterJson));

    imageApi.imageDelete("test:build", null, null);
    imageApi.imageDelete("test:build-base", null, null);
  }

  InputStream newBuildContext(File baseDirectory) throws IOException {
    ByteArrayOutputStream buildContext = new ByteArrayOutputStream();
    BuildContextBuilder.archiveTarFilesRecursively(baseDirectory, buildContext);
    return new ByteArrayInputStream(buildContext.toByteArray());
  }

  @Test
  public void imageCreatePullFromRemote() {
    assertDoesNotThrow(() -> imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null));
  }

  @Test
  public void imageCreateImportFromUrl() throws IOException {
    File tarFile = imageApi.imageGet(testImage.getImageWithTag());
    File destDir = new TarUtil().unTar(tarFile);
    File rootLayerTar = new ManifestUtil().getRootLayerLocation(destDir);
    URL importUrl = rootLayerTar.toURI().toURL();
    HttpTestServer server = new HttpTestServer();
    InetSocketAddress serverAddress = server.start("/images/", new HttpTestServer.FileServer(importUrl));
    int port = serverAddress.getPort();
    List<String> addresses = new NetworkInterfaces().getInet4Addresses();
    String url = String.format("http://%s:%s/images/%s", addresses.get(0), port, importUrl.getPath());

    assertDoesNotThrow(() -> imageApi.imageCreate(null, url, "test", "from-url", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, null));

    server.stop();
    imageApi.imageDelete("test:from-url", null, null);
  }

  @Test
  public void imageCreateImportFromInputStream() throws IOException {
    File tarFile = imageApi.imageGet(testImage.getImageWithTag());
    File destDir = new TarUtil().unTar(tarFile);
    File rootLayerTar = new ManifestUtil().getRootLayerLocation(destDir);
    try (InputStream source = new FileInputStream(rootLayerTar)) {
      assertDoesNotThrow(() -> imageApi.imageCreate(null, "-", "test", "from-stream", null, null, singletonList(String.format("LABEL %1$s=\"%2$s\"", LABEL_KEY, LABEL_VALUE)), null, source));
    }
    imageApi.imageDelete("test:from-stream", null, null);
  }

  @Test
  public void imageCommit() {
    imageApi.imageTag(testImage.getImageWithTag(), "test", "commit");
    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        singletonList("-"),
        null,
        null,
        "test:commit",
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    ContainerCreateResponse container = containerApi.containerCreate(containerCreateRequest, "container-commit-test");
    IdResponse image = imageApi.imageCommit(container.getId(), "test", "committed", null, null, null, null, null);
    assertTrue(image.getId().matches("sha256:\\w+"));
    imageApi.imageDelete("test:committed", null, null);
    containerApi.containerDelete("container-commit-test", null, null, null);
    imageApi.imageDelete("test:commit", null, null);
  }

  @Test
  public void imageList() {
    List<ImageSummary> images = imageApi.imageList(null, null, null);
    assertEquals(1, images.stream().filter((i) -> i.getRepoTags() != null && i.getRepoTags().stream().filter((t) -> t.equals(testImage.getImageWithTag())).count() > 0).count());
  }

  @Test
  public void imageDelete() {
    imageApi.imageTag(testImage.getImageWithTag(), "test", "delete");
    List<ImageDeleteResponseItem> deletedImages = imageApi.imageDelete("test:delete", null, null);
    assertTrue(deletedImages.stream().anyMatch((e) -> e.getDeleted() != null || e.getUntagged() != null));
  }

  @Test
  public void imageGet() throws IOException {
    imageApi.imageTag(testImage.getImageWithTag(), "test", "export");
    File exportedImage = imageApi.imageGet("test:export");
    assertTrue(16896 < Long.parseLong(readAttributes(exportedImage.toPath(), "size", NOFOLLOW_LINKS).get("size").toString()));

    imageApi.imageDelete("test:export", null, null);
  }

  @Test
  public void imageGetAll() throws IOException {
    imageApi.imageTag(testImage.getImageWithTag(), "test", "export-all-1");
    imageApi.imageTag(testImage.getImageWithTag(), "test", "export-all-2");

    File exportedImages = imageApi.imageGetAll(asList("test:export-all-1", "test:export-all-2"));
    assertTrue(22016 < Long.parseLong(readAttributes(exportedImages.toPath(), "size", NOFOLLOW_LINKS).get("size").toString()));

    imageApi.imageDelete("test:export-all-1", null, null);
    imageApi.imageDelete("test:export-all-2", null, null);
  }

  @Test
  public void imageLoad() {
    List<String> originalRepoDigests = imageApi.imageInspect(testImage.getImageWithTag()).getRepoDigests();

    imageApi.imageTag(testImage.getImageWithTag(), "test", "load-image");
    File tarFile = imageApi.imageGet("test:load-image");
    imageApi.imageDelete("test:load-image", null, null);

    assertDoesNotThrow(() -> imageApi.imageLoad(false, tarFile));

    List<String> actualRepoDigests = imageApi.imageInspect("test:load-image").getRepoDigests();
    assertEquals(originalRepoDigests, actualRepoDigests);

    imageApi.imageDelete("test:load-image", null, null);
  }

  @Test
  public void imageHistory() {
    imageApi.imageTag(testImage.getImageWithTag(), "test", "history");

    List<HistoryResponseItem> history = imageApi.imageHistory("test:history");
    assertFalse(history.isEmpty());
    Optional<HistoryResponseItem> historyItem = history.stream().filter(h -> h.getCreatedBy().contains("ENTRYPOINT [\"/main\"")).findFirst();
    assertTrue(historyItem.isPresent());

    imageApi.imageDelete("test:history", null, null);
  }

  @Test
  public void imageInspect() {
    imageApi.imageTag(testImage.getImageWithTag(), "test", "inspect");

    Image image = imageApi.imageInspect("test:inspect");
    assertTrue(image.getId().startsWith("sha256:"));

    imageApi.imageDelete("test:inspect", null, null);
  }

  @Test
  public void imageSearch() {
    List<ImageSearchResponseItem> searchResult = imageApi.imageSearch("alpine", 1, null);
    assertEquals(1, searchResult.size());
    assertEquals("alpine", searchResult.get(0).getName());
  }

  @Test
  public void imageTag() {
    imageApi.imageTag(testImage.getImageWithTag(), "test/image", "test-tag");
    Image image1 = imageApi.imageInspect(testImage.getImageWithTag());
    Image image2 = imageApi.imageInspect("test/image:test-tag");
    assertFalse(image1.getId().isEmpty());
    assertEquals(image1.getId(), image2.getId());

    imageApi.imageDelete("test/image:test-tag", null, null);
  }

  @Test
  public void imagePushToCustomRegistry() {
    DockerRegistry registry = new DockerRegistry(typeSafeDockerClient);
    registry.run();
    String registryUrl = registry.url();

    imageApi.imageTag(testImage.getImageWithTag(), registryUrl + "/test", "push");

    imageApi.imagePush(registryUrl + "/test", "", "push");

    registry.rm();

    imageApi.imageDelete(registryUrl + "/test:push", null, null);
  }
}
