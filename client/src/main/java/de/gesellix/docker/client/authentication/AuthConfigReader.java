package de.gesellix.docker.client.authentication;

import com.squareup.moshi.Moshi;
import de.gesellix.docker.engine.DockerEnv;
import okio.Okio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

import static de.gesellix.docker.client.authentication.AuthConfig.EMPTY_AUTH_CONFIG;

public class AuthConfigReader {

  private final static Logger log = LoggerFactory.getLogger(AuthConfigReader.class);

  private final Moshi moshi = new Moshi.Builder().build();

  private final DockerEnv env;

  public AuthConfigReader() {
    this(new DockerEnv());
  }

  public AuthConfigReader(DockerEnv env) {
    this.env = env;
  }

  //  @Override
  AuthConfig readDefaultAuthConfig() {
    return readAuthConfig(null, env.getDockerConfigFile());
  }

  //  @Override
  AuthConfig readAuthConfig(String hostname, File dockerCfg) {
    log.debug("read authConfig");

    if (hostname == null || hostname.trim().isEmpty()) {
      hostname = env.getIndexUrl_v1();
    }

    Map parsedDockerCfg = readDockerConfigFile(dockerCfg);
    if (parsedDockerCfg == null || parsedDockerCfg.isEmpty()) {
      return EMPTY_AUTH_CONFIG;
    }

    CredsStore credsStore = getCredentialsStore(parsedDockerCfg, hostname);
    return credsStore.getAuthConfig(hostname);
  }

  Map readDockerConfigFile(File dockerCfg) {
    if (dockerCfg == null) {
      dockerCfg = env.getDockerConfigFile();
    }
    if (dockerCfg == null || !dockerCfg.exists()) {
      log.info("docker config '${dockerCfg}' doesn't exist");
      return Collections.emptyMap();
    }
    log.debug("reading auth info from {}", dockerCfg);
    try {
      return moshi.adapter(Map.class).fromJson(Okio.buffer(Okio.source(dockerCfg)));
    }
    catch (Exception e) {
      log.debug(MessageFormat.format("failed to read auth info from {}", dockerCfg), e);
      return Collections.emptyMap();
    }
  }

  CredsStore getCredentialsStore(Map parsedDockerCfg) {
    return getCredentialsStore(parsedDockerCfg, "");
  }

  CredsStore getCredentialsStore(Map parsedDockerCfg, String hostname) {
    if (parsedDockerCfg.containsKey("credHelpers") && hostname != null && !hostname.trim().isEmpty()) {
      return new NativeStore((String) ((Map) parsedDockerCfg.get("credHelpers")).get(hostname));
    }
    if (parsedDockerCfg.containsKey("credsStore")) {
      return new NativeStore((String) parsedDockerCfg.get("credsStore"));
    }
    return new FileStore(parsedDockerCfg);
  }
}
