package de.gesellix.docker.client.authentication;

import java.util.Objects;

public class AuthConfig {

  public static final AuthConfig EMPTY_AUTH_CONFIG = new AuthConfig();

  private String username;
  private String password;
  private String auth;
  /**
   * Email is an optional value associated with the username.
   *
   * @deprecated This field is deprecated and will be removed in a later version of docker.
   */
  @Deprecated
  private String email;
  private String serveraddress;
  private String identitytoken;
  private String registrytoken;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getAuth() {
    return auth;
  }

  public void setAuth(String auth) {
    this.auth = auth;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getServeraddress() {
    return serveraddress;
  }

  public void setServeraddress(String serveraddress) {
    this.serveraddress = serveraddress;
  }

  public String getIdentitytoken() {
    return identitytoken;
  }

  public void setIdentitytoken(String identitytoken) {
    this.identitytoken = identitytoken;
  }

  public String getRegistrytoken() {
    return registrytoken;
  }

  public void setRegistrytoken(String registrytoken) {
    this.registrytoken = registrytoken;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {return true;}
    if (o == null || getClass() != o.getClass()) {return false;}
    AuthConfig that = (AuthConfig) o;
    return Objects.equals(username, that.username) && Objects.equals(password, that.password) && Objects.equals(auth, that.auth) &&
           Objects.equals(email, that.email) && Objects.equals(serveraddress, that.serveraddress) && Objects.equals(identitytoken, that.identitytoken) &&
           Objects.equals(registrytoken, that.registrytoken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, password, auth, email, serveraddress, identitytoken, registrytoken);
  }
}
