package de.gesellix.util

import groovy.json.JsonBuilder

class QueryUtil {

  void applyDefaults(Map<String, ?> query, Map<String, ?> defaults) {
    defaults.each { String k, Object v ->
      if (!query.containsKey(k)) {
        query.put(k, v)
      }
    }
  }

  void jsonEncodeFilters(Map<String, ?> query) {
    jsonEncodeQueryParameter(query, "filters")
  }

  void jsonEncodeBuildargs(Map<String, ?> query) {
    jsonEncodeQueryParameter(query, "buildargs")
  }

  void jsonEncodeQueryParameter(Map<String, ?> query, String parameterName) {
    query.each { String k, Object v ->
      if (k == parameterName) {
        query.put(k, new JsonBuilder(v).toString())
      }
    }
  }
}
