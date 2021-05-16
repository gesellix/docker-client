package de.gesellix.docker.client.testutil;

import de.gesellix.docker.client.TypeSafeDockerClientImpl;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledIfDaemonOnWindowsOs.WindowsDaemonCondition.class)
public @interface DisabledIfDaemonOnWindowsOs {

  class WindowsDaemonCondition implements ExecutionCondition {

    private final TypeSafeDockerClientImpl dockerClient;

    public WindowsDaemonCondition() {
      dockerClient = new TypeSafeDockerClientImpl();
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
      return isWindowsDaemon()
             ? disabled("Disabled: Windows daemon detected")
             : enabled("Enabled: Non-Windows daemon detected");
    }

    public boolean isWindowsDaemon() {
      return Objects.requireNonNull(dockerClient.getSystemApi().systemVersion().getOs())
          .equalsIgnoreCase("windows");
    }
  }
}
