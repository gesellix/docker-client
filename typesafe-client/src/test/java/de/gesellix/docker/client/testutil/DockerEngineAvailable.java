package de.gesellix.docker.client.testutil;

import de.gesellix.docker.client.TypeSafeDockerClientImpl;
import de.gesellix.docker.remote.api.LocalNodeState;
import de.gesellix.docker.remote.api.core.LoggingExtensionsKt;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.slf4j.Logger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@ExtendWith(DockerEngineAvailable.DockerEngineAvailableCondition.class)
public @interface DockerEngineAvailable {

  Logger log = LoggingExtensionsKt.logger(DockerEngineAvailable.class.getName()).getValue();

  LocalNodeState requiredSwarmMode() default LocalNodeState.EMPTY;

  class DockerEngineAvailableCondition implements ExecutionCondition, TestInstancePostProcessor, TestInstancePreDestroyCallback {

    private final TypeSafeDockerClientImpl dockerClient;
    private final SwarmUtil swarmUtil;
    private LocalNodeState requiredSwarmMode;
    private LocalNodeState previousNodeState = LocalNodeState.EMPTY;

    public DockerEngineAvailableCondition() {
      dockerClient = new TypeSafeDockerClientImpl();
      swarmUtil = new SwarmUtil(dockerClient);
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
      return available()
             ? enabled("Docker Engine is available")
             : disabled("Docker Engine is not available");
    }

    boolean available() {
      try {
        dockerClient.getSystemApi().systemPing();
        return true;
      }
      catch (Exception e) {
        log.warn("Docker not available");
        return false;
      }
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
      initDockerClientField(testInstance);

      requiredSwarmMode = testInstance.getClass().getAnnotation(DockerEngineAvailable.class).requiredSwarmMode();
      switch (requiredSwarmMode) {
        case Active:
          previousNodeState = swarmUtil.ensureActiveSwarm();
          break;
        case Inactive:
          previousNodeState = swarmUtil.ensureInactiveSwarm();
          break;
        case EMPTY:
          // don't change anything
          break;
        default:
          throw new ExtensionConfigurationException("Not supported: " + requiredSwarmMode);
      }
    }

    private void initDockerClientField(Object testInstance) throws IllegalAccessException {
      List<Field> fields = ReflectionSupport.findFields(testInstance.getClass(), (Field f) -> f.getAnnotation(InjectDockerClient.class) != null, HierarchyTraversalMode.TOP_DOWN);
      if (fields.isEmpty()) {
        throw new ExtensionConfigurationException(String.format("No %1$s annotated field found in %2$s.", InjectDockerClient.class, testInstance.getClass()));
      }
      if (fields.size() > 1) {
        throw new ExtensionConfigurationException(String.format("Multiple fields annotated with %1$s found in %2$s.", InjectDockerClient.class, testInstance.getClass()));
      }
      Field dockerClientField = fields.get(0);
      dockerClientField.setAccessible(true);
      dockerClientField.set(testInstance, dockerClient);
    }

    @Override
    public void preDestroyTestInstance(ExtensionContext context) {
      if (previousNodeState != requiredSwarmMode) {
        switch (previousNodeState) {
          case Active:
            swarmUtil.ensureActiveSwarm();
            break;
          case Inactive:
            swarmUtil.ensureInactiveSwarm();
            break;
          default:
            log.warn("Won't revert LocalNodeState back to {}", previousNodeState);
        }
      }
    }
  }
}
