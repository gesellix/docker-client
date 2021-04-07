package de.gesellix.docker.client.authentication

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class CredsStoreHelperResult {

  String error
  Map<String, Object> data
}
