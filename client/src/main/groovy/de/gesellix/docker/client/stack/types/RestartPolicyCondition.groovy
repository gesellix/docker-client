package de.gesellix.docker.client.stack.types

enum RestartPolicyCondition {

  RestartPolicyConditionNone("none"),
  RestartPolicyConditionOnFailure("on-failure"),
  RestartPolicyConditionAny("any")

  final String value

  RestartPolicyCondition(String value) {
    this.value = value
  }

  static RestartPolicyCondition byValue(String needle) {
    def entry = values().find { it.value == needle }
    if (!entry) {
      throw new IllegalArgumentException("no enum found for ${needle}")
    }
    return entry
  }
}
