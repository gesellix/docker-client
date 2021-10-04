package de.gesellix.docker.client;

import com.squareup.moshi.Moshi;
import de.gesellix.docker.client.testutil.DisabledIfDaemonOnWindowsOs;
import de.gesellix.docker.client.testutil.DockerEngineAvailable;
import de.gesellix.docker.client.testutil.InjectDockerClient;
import de.gesellix.docker.client.testutil.TestImage;
import de.gesellix.docker.engine.api.Cancellable;
import de.gesellix.docker.engine.api.Frame;
import de.gesellix.docker.engine.api.ServiceApi;
import de.gesellix.docker.engine.api.StreamCallback;
import de.gesellix.docker.engine.client.infrastructure.LoggingExtensionsKt;
import de.gesellix.docker.remote.api.EndpointPortConfig;
import de.gesellix.docker.remote.api.EndpointSpec;
import de.gesellix.docker.remote.api.LocalNodeState;
import de.gesellix.docker.remote.api.Service;
import de.gesellix.docker.remote.api.ServiceServiceStatus;
import de.gesellix.docker.remote.api.ServiceSpec;
import de.gesellix.docker.remote.api.ServiceSpecMode;
import de.gesellix.docker.remote.api.ServiceSpecModeReplicated;
import de.gesellix.docker.remote.api.ServiceSpecUpdateConfig;
import de.gesellix.docker.remote.api.ServiceUpdateResponse;
import de.gesellix.docker.remote.api.ServiceUpdateStatus;
import de.gesellix.docker.remote.api.TaskSpec;
import de.gesellix.docker.remote.api.TaskSpecContainerSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static de.gesellix.docker.client.testutil.Constants.LABEL_KEY;
import static de.gesellix.docker.client.testutil.Constants.LABEL_VALUE;
import static de.gesellix.docker.remote.api.EndpointPortConfig.Protocol.Tcp;
import static de.gesellix.docker.remote.api.EndpointPortConfig.PublishMode.Ingress;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DockerEngineAvailable(requiredSwarmMode = LocalNodeState.Active)
class ServiceApiIntegrationTest {

  private static final Logger log = LoggingExtensionsKt.logger(ServiceApiIntegrationTest.class.getName()).getValue();

  @InjectDockerClient
  private TypeSafeDockerClientImpl typeSafeDockerClient;

  private TestImage testImage;

  ServiceApi serviceApi;

  @BeforeEach
  public void setup() {
    serviceApi = typeSafeDockerClient.getServiceApi();
    testImage = new TestImage(typeSafeDockerClient);
  }

  @Test
  public void serviceList() {
    List<Service> services = serviceApi.serviceList(null, null);
    assertNotNull(services);
  }

  @DisabledIfDaemonOnWindowsOs
  @Test
  public void serviceLogs() throws InterruptedException {
    // TODO fails on Windows
    // "failed during hnsCallRawResponse: hnsCall failed in Win32: The parameter is incorrect. (0x57)"
    // - https://github.com/moby/moby/issues/40621
    // - https://github.com/moby/moby/issues/41094

    serviceApi.serviceCreate(
        new ServiceSpec("test-service", singletonMap(LABEL_KEY, LABEL_VALUE),
                        new TaskSpec(null, new TaskSpecContainerSpec(testImage.getImageWithTag(), singletonMap(LABEL_KEY, LABEL_VALUE),
                                                                     null, null, null, null, null,
                                                                     null, null, null,
                                                                     null, null, null, null,
                                                                     null, null, null,
                                                                     null, null, null, null,
                                                                     null, null, null, null, null, null),
                                     null, null, null, null, null, null, null, null),
                        new ServiceSpecMode(new ServiceSpecModeReplicated(1L), null, null, null),
                        new ServiceSpecUpdateConfig(1L, null, null, null, null, null),
                        null, null,
                        new EndpointSpec(null, singletonList(new EndpointPortConfig(null, Tcp, 8080, 8080, Ingress)))),
        null);

    CountDownLatch wait1 = new CountDownLatch(1);
    Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        Map<String, List<String>> filter = new HashMap<>();
        filter.put("name", singletonList("test-service"));
        String filterJson = new Moshi.Builder().build().adapter(Map.class).toJson(filter);
        ServiceServiceStatus serviceStatus = serviceApi.serviceList(filterJson, true).get(0).getServiceStatus();
        if (serviceStatus != null && serviceStatus.getRunningTasks() > 0) {
          wait1.countDown();
          timer.cancel();
        }
      }
    }, 500, 500);
    wait1.await(10, TimeUnit.SECONDS);
    timer.cancel();

    Duration timeout = Duration.of(5, SECONDS);
    LogStreamCallback callback = new LogStreamCallback();

    new Thread(() -> serviceApi.serviceLogs(
        "test-service",
        null, null, true, true, null, null, null,
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
    serviceApi.serviceDelete("test-service");
    assertSame(callback.frames.stream().findAny().get().getStreamType(), Frame.StreamType.STDOUT);
  }

  @Test
  public void serviceCreateInspectUpdateDelete() throws InterruptedException {
    serviceApi.serviceCreate(new ServiceSpec("test-service", singletonMap(LABEL_KEY, LABEL_VALUE),
                                             new TaskSpec(null, new TaskSpecContainerSpec(testImage.getImageWithTag(), singletonMap(LABEL_KEY, LABEL_VALUE),
                                                                                          null, null, null, null, null,
                                                                                          null, null, null,
                                                                                          null, null, null, null,
                                                                                          null, null, null,
                                                                                          null, null, null, null,
                                                                                          null, null, null, null, null, null),
                                                          null, null, null, null, null, null, null, null),
                                             new ServiceSpecMode(new ServiceSpecModeReplicated(1L), null, null, null),
                                             new ServiceSpecUpdateConfig(1L, null, null, null, null, null),
                                             null, null,
                                             new EndpointSpec(null, singletonList(new EndpointPortConfig(null, Tcp, 8080, 8080, Ingress)))),
                             null);
    // TODO TGe: we try to wait a bit to work around an error like '{"message":"rpc error: code = Unknown desc = update out of sequence"}'
    Thread.sleep(5000);
    Service serviceInspect = serviceApi.serviceInspect("test-service", false);
    Integer serviceVersion = serviceInspect.getVersion().getIndex();
    assertTrue(serviceVersion > 0);

    Map<String, String> labels = new HashMap<>();
    labels.putAll(serviceInspect.getSpec().getLabels());
    labels.put("another-label", "another-value");
    ServiceSpec spec = new ServiceSpec("test-service", labels,
                                       new TaskSpec(null, new TaskSpecContainerSpec(testImage.getImageWithTag(), labels,
                                                                                    null, null, null, null, null,
                                                                                    null, null, null,
                                                                                    null, null, null, null,
                                                                                    null, null, null,
                                                                                    null, null, null, null,
                                                                                    null, null, null, null, null, null),
                                                    null, null, null, null, null, null, null, null),
                                       new ServiceSpecMode(new ServiceSpecModeReplicated(1L), null, null, null),
                                       new ServiceSpecUpdateConfig(1L, null, null, null, null, null),
                                       null, null,
                                       new EndpointSpec(null, singletonList(new EndpointPortConfig(null, Tcp, 8080, 8080, Ingress))));
    ServiceUpdateResponse updateResponse = serviceApi.serviceUpdate("test-service", serviceVersion, spec, null, null, null);
    assertTrue(updateResponse.getWarnings() == null || updateResponse.getWarnings().isEmpty());

    CountDownLatch wait = new CountDownLatch(1);
    Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        ServiceUpdateStatus updateStatus = serviceApi.serviceInspect("test-service", false).getUpdateStatus();
        if (updateStatus.getState() == ServiceUpdateStatus.State.Completed) {
          wait.countDown();
          timer.cancel();
        }
      }
    }, 500, 500);
    wait.await(10, TimeUnit.SECONDS);
    Service serviceInspect2 = serviceApi.serviceInspect("test-service", false);
    assertEquals("another-value", serviceInspect2.getSpec().getLabels().get("another-label"));
    serviceApi.serviceDelete("test-service");
  }

  static class LogStreamCallback implements StreamCallback<Frame> {

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
}
