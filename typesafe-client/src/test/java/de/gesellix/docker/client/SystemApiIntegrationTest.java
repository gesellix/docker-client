package de.gesellix.docker.client;

import de.gesellix.docker.authentication.AuthConfigReader;
import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.client.testutil.TestImage;
import de.gesellix.docker.engine.api.Cancellable;
import de.gesellix.docker.engine.api.ContainerApi;
import de.gesellix.docker.engine.api.ImageApi;
import de.gesellix.docker.engine.api.StreamCallback;
import de.gesellix.docker.engine.api.SystemApi;
import de.gesellix.docker.engine.client.infrastructure.ClientException;
import de.gesellix.docker.engine.client.infrastructure.LoggingExtensionsKt;
import de.gesellix.docker.remote.api.AuthConfig;
import de.gesellix.docker.remote.api.EventMessage;
import de.gesellix.docker.remote.api.SystemAuthResponse;
import de.gesellix.docker.remote.api.SystemInfo;
import de.gesellix.docker.remote.api.SystemVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DockerEngineAvailable
class SystemApiIntegrationTest {

  private static final Logger log = LoggingExtensionsKt.logger(SystemApiIntegrationTest.class.getName()).getValue();

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  private TestImage testImage;

  SystemApi systemApi;
  ImageApi imageApi;
  ContainerApi containerApi;

  @BeforeEach
  public void setup() {
    systemApi = typeSafeDockerClient.getSystemApi();
    imageApi = typeSafeDockerClient.getImageApi();
    containerApi = typeSafeDockerClient.getContainerApi();
    testImage = new TestImage(typeSafeDockerClient);
  }

  @Test
  public void systemAuthWhenUnauthorized() {
    assertThrows(ClientException.class, () -> systemApi.systemAuth(new AuthConfig("unknown-username", "a-secret", "user@example.com", null)));
  }

  @Test
  public void systemAuthWhenAuthorized() {
    de.gesellix.docker.authentication.AuthConfig defaultAuthConfig = new AuthConfigReader().readDefaultAuthConfig();
    SystemAuthResponse authResponse = systemApi.systemAuth(new AuthConfig(defaultAuthConfig.getUsername(), defaultAuthConfig.getPassword(), null, null));
    assertEquals("Login Succeeded", authResponse.getStatus());
  }

  @Test
  public void systemDataUsage() {
    assertDoesNotThrow(() -> systemApi.systemDataUsage());
  }

  @Test
  public void systemEvents() {
    Duration timeout = Duration.of(20, SECONDS);
    Instant since = ZonedDateTime.now().toInstant();
    Instant until = ZonedDateTime.now().plus(timeout).plusSeconds(10).toInstant();
    SystemEventsCallback callback = new SystemEventsCallback();

    new Thread(() -> systemApi.systemEvents(
        "" + since.getEpochSecond(),
        "" + until.getEpochSecond(),
        null,
        callback,
        timeout.toMillis())).start();

    try {
      Thread.sleep(10);
    }
    catch (InterruptedException e) {
      log.warn("ignoring interrupted wait", e);
    }

    imageApi.imageTag(testImage.getImageWithTag(), "test", "system-events");
    imageApi.imageDelete("test:system-events", null, null);

    CountDownLatch wait = new CountDownLatch(1);
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        callback.job.cancel();
        wait.countDown();
      }
    }, 1000);
    try {
      wait.await();
    }
    catch (InterruptedException e) {
      log.warn("interrupted", e);
    }

    EventMessage event = callback.events.stream().filter(e -> Objects.equals(e.getAction(), "tag")).findFirst().orElse(new EventMessage());
    assertEquals(EventMessage.Type.Image, event.getType());
  }

  @Test
  public void systemInfo() {
    SystemInfo systemInfo = systemApi.systemInfo();
    assertEquals("docker.io", systemInfo.getRegistryConfig().getIndexConfigs().get("docker.io").getName());
    assertTrue(systemInfo.getRegistryConfig().getIndexConfigs().get("docker.io").getOfficial());
    assertTrue(systemInfo.getRegistryConfig().getIndexConfigs().get("docker.io").getSecure());
    assertTrue(null == systemInfo.getIsolation() || systemInfo.getIsolation() == SystemInfo.Isolation.Hyperv);
  }

  @Test
  public void systemPing() {
    String systemPing = systemApi.systemPing();
    assertEquals("OK", systemPing);
  }

  @Test
  public void systemPingHead() {
    String systemPing = systemApi.systemPingHead();
    assertEquals("", systemPing);
  }

  @Test
  public void systemVersion() {
    SystemVersion systemVersion = systemApi.systemVersion();
    // will break on CI or in other environments - TODO fixme
    assertEquals("1.41", systemVersion.getApiVersion());
  }

  static class SystemEventsCallback implements StreamCallback<EventMessage> {

    List<EventMessage> events = new ArrayList<>();
    Cancellable job = null;

    @Override
    public void onStarting(Cancellable cancellable) {
      job = cancellable;
    }

    @Override
    public void onNext(EventMessage event) {
      events.add(event);
      log.info("{}", event);
    }
  }
}
