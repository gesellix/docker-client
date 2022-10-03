package de.gesellix.docker.client.container;

import de.gesellix.docker.engine.EngineResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DockerResponseHandler {

  public void ensureSuccessfulResponse(EngineResponse response, Throwable throwable) throws Throwable {
    if (response == null || !response.getStatus().getSuccess() || hasError(response)) {
      logError(response);
      throw throwable;
    }
  }

  public void logError(final EngineResponse response) {
    if (response != null && response.getContent() instanceof String) {
      log.error("request failed: '{}'", response.getContent());
    } else {
      log.error("request failed: {}", getErrors(response));
    }
  }

  public boolean hasError(EngineResponse response) {
    return getErrors(response).size() > 0;
  }

  public List<Object> getErrors(final EngineResponse response) {
    if (response == null || response.getContent() == null) {
      return Collections.emptyList();
    }

    if (response.getMimeType() == null || response.getMimeType().equals("application/json")) {
      List<Object> foundErrors = new ArrayList<>();
      if (response.getContent() instanceof List) {
        List content = (List) response.getContent();
        content.forEach((Object element) -> {
          if (element instanceof Map) {
            foundErrors.add(((Map<String, Object>) element).get("error"));
          } else if (element != null) {
            foundErrors.add(element.toString());
          }
        });
      } else if (response.getContent() instanceof Map) {
        Map<String, Object> content = (Map<String, Object>) response.getContent();
        if (content.containsKey("error")) {
          foundErrors.add(content.get("error"));
        } else if (content.containsKey("message")) {
          foundErrors.add(content.get("message"));
        }
      } else {
        log.debug("won't search for errors in {}", response.getContent().getClass());
      }

      return foundErrors;
    }

    return Collections.emptyList();
  }

  private final Logger log = LoggerFactory.getLogger(DockerResponseHandler.class);
}
