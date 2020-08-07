package de.gesellix.docker.client.authentication

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class AuthConfig {

    String username
    String password
    String auth

    /**
     * Email is an optional value associated with the username.
     * @deprecated This field is deprecated and will be removed in a later version of docker.
     */
    @Deprecated
    String email

    String serveraddress

    // IdentityToken is used to authenticate the user and get an access token for the registry.
    String identitytoken

    // RegistryToken is a bearer token to be sent to a registry
    String registrytoken
}
