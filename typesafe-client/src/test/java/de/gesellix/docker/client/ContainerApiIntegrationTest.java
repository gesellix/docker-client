package de.gesellix.docker.client;

import com.squareup.moshi.Moshi;
import de.gesellix.docker.client.testutil.DisabledIfDaemonOnWindowsOs;
import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.Failsafe;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.client.testutil.TestImage;
import de.gesellix.docker.engine.api.Cancellable;
import de.gesellix.docker.engine.api.ContainerApi;
import de.gesellix.docker.engine.api.Frame;
import de.gesellix.docker.engine.api.ImageApi;
import de.gesellix.docker.engine.api.StreamCallback;
import de.gesellix.docker.engine.client.infrastructure.ClientException;
import de.gesellix.docker.engine.client.infrastructure.LoggingExtensionsKt;
import de.gesellix.docker.engine.model.ContainerCreateRequest;
import de.gesellix.docker.engine.model.ContainerCreateResponse;
import de.gesellix.docker.engine.model.ContainerInspectResponse;
import de.gesellix.docker.engine.model.ContainerPruneResponse;
import de.gesellix.docker.engine.model.ContainerTopResponse;
import de.gesellix.docker.engine.model.ContainerUpdateRequest;
import de.gesellix.docker.engine.model.ContainerUpdateResponse;
import de.gesellix.docker.engine.model.RestartPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static de.gesellix.docker.client.testutil.Constants.LABEL_KEY;
import static de.gesellix.docker.client.testutil.Constants.LABEL_VALUE;
import static de.gesellix.docker.client.testutil.Failsafe.removeContainer;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DockerEngineAvailable
class ContainerApiIntegrationTest {

  private static final Logger log = LoggingExtensionsKt.logger(ContainerApiIntegrationTest.class.getName()).getValue();

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  private TestImage testImage;

  ContainerApi containerApi;
  ImageApi imageApi;

  @BeforeEach
  public void setup() {
    containerApi = typeSafeDockerClient.getContainerApi();
    imageApi = typeSafeDockerClient.getImageApi();
    testImage = new TestImage(typeSafeDockerClient);
  }

  @Test
  public void containerList() {
    List<Map<String, Object>> containers = containerApi.containerList(null, null, null, null);
    assertNotNull(containers);
  }

  @Test
  public void containerCreate() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        singletonList("-"),
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    ContainerCreateResponse container = containerApi.containerCreate(containerCreateRequest, "container-create-test");
    assertTrue(container.getId().matches("\\w+"));

    removeContainer(typeSafeDockerClient, "container-create-test");
  }

  @Test
  public void containerDelete() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        singletonList("-"),
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-delete-test");
    assertDoesNotThrow(() -> containerApi.containerDelete("container-delete-test", null, null, null));
    assertDoesNotThrow(() -> containerApi.containerDelete("container-delete-missing", null, null, null));

    Failsafe.perform(() -> imageApi.imageDelete(testImage.getImageWithTag(), null, null));
  }

  // WCOW does not support exporting containers
  // See https://github.com/moby/moby/issues/33581
  @DisabledIfDaemonOnWindowsOs
  @Test
  public void containerExport() {
    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-export");
    assertDoesNotThrow(() -> containerApi.containerExport("container-export"));
    removeContainer(typeSafeDockerClient, "container-export");
  }

  @Test
  public void containerInspect() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        singletonList("-"),
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-inspect-test");
    ContainerInspectResponse container = containerApi.containerInspect("container-inspect-test", false);
    assertEquals("/container-inspect-test", container.getName());

    removeContainer(typeSafeDockerClient, "container-inspect-test");
  }

  @Test
  public void containerInspectMissing() {
    ClientException clientException = assertThrows(ClientException.class, () -> containerApi.containerInspect("random-" + UUID.randomUUID(), false));
    assertEquals(404, clientException.getStatusCode());
  }

  @Test
  public void containerRename() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        singletonList("-"),
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-rename-test");
    assertDoesNotThrow(() -> containerApi.containerRename("container-rename-test", "fancy-name"));

    removeContainer(typeSafeDockerClient, "fancy-name");
  }

  @Test
  public void containerStartStopWait() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-start-test");
    containerApi.containerStart("container-start-test", null);
    ContainerInspectResponse container = containerApi.containerInspect("container-start-test", false);
    assertTrue(container.getState().getRunning());

    removeContainer(typeSafeDockerClient, "container-start-test");
  }

  @Test
  public void containerLogsWithoutTty() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-logs-test");
    containerApi.containerStart("container-logs-test", null);

    Duration timeout = Duration.of(5, SECONDS);
    LogFrameStreamCallback callback = new LogFrameStreamCallback();

    new Thread(() -> containerApi.containerLogs(
        "container-logs-test",
        false, true, true, null, null, null, null,
        callback, timeout.toMillis())).start();

    CountDownLatch wait = new CountDownLatch(1);
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        callback.job.cancel();
        wait.countDown();
      }
    }, 5000);

    try {
      wait.await();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    Optional<Frame> frame = callback.frames.stream().findAny();
    assertTrue(frame.isPresent());
    assertSame(frame.get().getStreamType(), Frame.StreamType.STDOUT);

    removeContainer(typeSafeDockerClient, "container-logs-test");
  }

  @Test
  public void containerLogsWithTty() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        true, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-logs-with-tty-test");
    containerApi.containerStart("container-logs-with-tty-test", null);

    Duration timeout = Duration.of(5, SECONDS);
    LogFrameStreamCallback callback = new LogFrameStreamCallback();

    new Thread(() -> containerApi.containerLogs(
        "container-logs-with-tty-test",
        false, true, true, null, null, null, null,
        callback, timeout.toMillis())).start();

    CountDownLatch wait = new CountDownLatch(1);
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        callback.job.cancel();
        wait.countDown();
      }
    }, 5000);

    try {
      wait.await();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    assertSame(callback.frames.stream().findAny().get().getStreamType(), Frame.StreamType.RAW);

    removeContainer(typeSafeDockerClient, "container-logs-with-tty-test");
  }

  @Test
  public void containerUpdate() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-update-test");
    containerApi.containerStart("container-update-test", null);
    ContainerUpdateRequest updateRequest = new ContainerUpdateRequest(
        null, null, null,
        null, null, null, null, null, null,
        null, null, null, null, null, null,
        null, null, null,
        null, null, null, null, null,
        null, null, null, null, null,
        null, null, null, null,
        new RestartPolicy(RestartPolicy.Name.UnlessMinusStopped, null));
    ContainerUpdateResponse updateResponse = containerApi.containerUpdate("container-update-test", updateRequest);
    assertTrue(updateResponse.getWarnings() == null || updateResponse.getWarnings().isEmpty());

    removeContainer(typeSafeDockerClient, "container-update-test");
  }

  @Test
  public void containerRestart() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-restart-test");
    containerApi.containerStart("container-restart-test", null);
    assertDoesNotThrow(() -> containerApi.containerRestart("container-restart-test", 5));

    removeContainer(typeSafeDockerClient, "container-restart-test");
  }

  @Test
  public void containerKill() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-kill-test");
    containerApi.containerStart("container-kill-test", null);
    assertDoesNotThrow(() -> containerApi.containerKill("container-kill-test", null));

    removeContainer(typeSafeDockerClient, "container-kill-test");
  }

  @Test
  public void containerPauseUnpause() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-pause-test");
    containerApi.containerStart("container-pause-test", null);
    assertDoesNotThrow(() -> containerApi.containerPause("container-pause-test"));
    ContainerInspectResponse pausedContainer = containerApi.containerInspect("container-pause-test", false);
    assertTrue(pausedContainer.getState().getPaused());
    assertDoesNotThrow(() -> containerApi.containerUnpause("container-pause-test"));
    ContainerInspectResponse unpausedContainer = containerApi.containerInspect("container-pause-test", false);
    assertFalse(unpausedContainer.getState().getPaused());

    removeContainer(typeSafeDockerClient, "container-pause-test");
  }

  @Test
  public void containerPrune() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-prune-test");

    Map<String, List<String>> filter = new HashMap<>();
    filter.put("label", singletonList(LABEL_KEY));
    String filterJson = new Moshi.Builder().build().adapter(Map.class).toJson(filter);

    Optional<Map<String, Object>> toBePruned = containerApi.containerList(true, null, null, filterJson).stream().filter((c) -> ((List<String>) c.get("Names")).contains("/container-prune-test")).findFirst();
    assertTrue(toBePruned.isPresent());

    ContainerPruneResponse pruneResponse = containerApi.containerPrune(filterJson);
    assertTrue(pruneResponse.getContainersDeleted().contains(toBePruned.get().get("Id")));

    Optional<Map<String, Object>> shouldBeMissing = containerApi.containerList(true, null, null, filterJson).stream().filter((c) -> ((List<String>) c.get("Names")).contains("/container-prune-test")).findFirst();
    assertFalse(shouldBeMissing.isPresent());

    removeContainer(typeSafeDockerClient, "container-prune-test");
  }

  // the api reference v1.41 says: "On Unix systems, this is done by running the ps command. This endpoint is not supported on Windows."
  @Test
  public void containerTop() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-top-test");
    containerApi.containerStart("container-top-test", null);

    ContainerTopResponse processes = containerApi.containerTop("container-top-test", null);
    final String processTitle;
    if (processes.getTitles().contains("CMD")) {
      // Linux, macOS
      processTitle = "CMD";
    }
    else {
      // Windows
      processTitle = "Name";
    }
    List<List<String>> mainProcesses = processes.getProcesses().stream().filter((p) -> p.get(processes.getTitles().indexOf(processTitle)).contains("main")).collect(Collectors.toList());

    assertEquals(1, mainProcesses.size());

    removeContainer(typeSafeDockerClient, "container-top-test");
  }

  @Test
  public void containerStatsStream() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-stats-test");
    containerApi.containerStart("container-stats-test", null);

    Duration timeout = Duration.of(5, SECONDS);
    LogObjectStreamCallback callback = new LogObjectStreamCallback();

    containerApi.containerStats("container-stats-test", null, null, callback, timeout.toMillis());
    assertFalse(callback.elements.isEmpty());

    removeContainer(typeSafeDockerClient, "container-stats-test");
  }

  @Test
  public void containerStatsOnce() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-stats-test");
    containerApi.containerStart("container-stats-test", null);

    Duration timeout = Duration.of(5, SECONDS);
    LogObjectStreamCallback callback = new LogObjectStreamCallback();

    containerApi.containerStats("container-stats-test", false, null, callback, timeout.toMillis());
    assertFalse(callback.elements.isEmpty());

    removeContainer(typeSafeDockerClient, "container-stats-test");
  }

  @Test
  public void containerAttachNonInteractive() {
    imageApi.imageCreate(testImage.getImageName(), null, null, testImage.getImageTag(), null, null, null, null, null);

    ContainerCreateRequest containerCreateRequest = new ContainerCreateRequest(
        null, null, null,
        false, false, false,
        null,
        false, null, null,
        null,
        null,
        null,
        null,
        testImage.getImageWithTag(),
        null, null, null,
        null, null,
        null,
        singletonMap(LABEL_KEY, LABEL_VALUE),
        null, null,
        null,
        null,
        null
    );
    containerApi.containerCreate(containerCreateRequest, "container-attach-non-interactive-test");
    containerApi.containerStart("container-attach-non-interactive-test", null);

    Duration timeout = Duration.of(5, SECONDS);
    LogFrameStreamCallback callback = new LogFrameStreamCallback();

    new Thread(() -> containerApi.containerAttach(
        "container-attach-non-interactive-test",
        null, true, true, null, true, true,
        callback, timeout.toMillis())).start();

    CountDownLatch wait = new CountDownLatch(1);
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        callback.job.cancel();
        wait.countDown();
      }
    }, 5000);

    try {
      wait.await();
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }
    assertSame(callback.frames.stream().findAny().get().getStreamType(), Frame.StreamType.STDOUT);

    removeContainer(typeSafeDockerClient, "container-attach-non-interactive-test");
  }

  static class LogFrameStreamCallback implements StreamCallback<Frame> {

    List<Frame> frames = new ArrayList<>();
    Cancellable job = null;

    @Override
    public void onStarting(Cancellable cancellable) {
      job = cancellable;
    }

    @Override
    public void onNext(Frame frame) {
      frames.add(frame);
      log.info("next: {}", frame);
    }
  }

  static class LogObjectStreamCallback implements StreamCallback<Object> {

    List<Object> elements = new ArrayList<>();
    Cancellable job = null;

    @Override
    public void onStarting(Cancellable cancellable) {
      job = cancellable;
    }

    @Override
    public void onNext(Object element) {
      elements.add(element);
      log.info("next: {}", element);
    }
  }
}
