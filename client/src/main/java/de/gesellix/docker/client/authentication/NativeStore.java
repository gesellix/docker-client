package de.gesellix.docker.client.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static de.gesellix.docker.client.authentication.AuthConfig.EMPTY_AUTH_CONFIG;

public class NativeStore implements CredsStore {

  private final static Logger log = LoggerFactory.getLogger(NativeStore.class);

  private final static String TOKEN_USERNAME = "<token>";

  private final String credStoreName;

  CredsStoreHelper credsStoreHelper;

  public NativeStore(String credStoreName) {
    this.credStoreName = credStoreName;
    this.credsStoreHelper = new CredsStoreHelper();
  }

  @Override
  public AuthConfig getAuthConfig(String registry) {
    CredsStoreHelperResult creds = credsStoreHelper.getAuthentication(credStoreName, registry);
    if (creds.getError() != null && !creds.getError().trim().isEmpty()) {
      log.info("Error reading credentials from 'credsStore={}' for authentication at {}: {}", credStoreName, registry, creds.getError());
      return EMPTY_AUTH_CONFIG;
    }
    else if (creds.getData() != null && !creds.getData().isEmpty()) {
      log.info("Got credentials from 'credsStore={}'", credStoreName);
      AuthConfig result = parseCreds(creds.getData());
      result.setServeraddress(registry);
      return result;
    }
    else {
      log.warn("Using 'credsStore={}' for authentication at {} is currently not supported", credStoreName, registry);
      return EMPTY_AUTH_CONFIG;
    }
  }

  @Override
  public Map<String, AuthConfig> getAuthConfigs() {
    final Map<String, AuthConfig> result = new HashMap<String, AuthConfig>();
    CredsStoreHelperResult creds = credsStoreHelper.getAllAuthentications(credStoreName);
    if (creds.getError() != null && !creds.getError().trim().isEmpty()) {
      log.info("Error reading credentials from 'credsStore={}': {}", credStoreName, creds.getError());
      return result;
    }
    else if (creds.getData() != null && !creds.getData().isEmpty()) {
      log.info("Got credentials from 'credsStore={}'", credStoreName);
      return creds.getData().keySet()
          .stream()
          .collect(Collectors.toMap(
              k -> k,
              this::getAuthConfig
          ));
    }
    else {
      log.warn("Using 'credsStore={}' is currently not supported", credStoreName);
      return result;
    }
  }

  private AuthConfig parseCreds(Map creds) {
    AuthConfig authDetails;
    if (TOKEN_USERNAME.equals(creds.get("Username"))) {
      authDetails = new AuthConfig();
      authDetails.setIdentitytoken((String) creds.get("Secret"));
    }
    else {
      authDetails = new AuthConfig();
      authDetails.setUsername((String) creds.get("Username"));
      authDetails.setPassword((String) creds.get("Secret"));
    }

    return authDetails;
  }

  private static <K, V, Value extends V> Value putAt0(Map<K, V> propOwner, K key, Value value) {
    propOwner.put(key, value);
    return value;
  }
}
