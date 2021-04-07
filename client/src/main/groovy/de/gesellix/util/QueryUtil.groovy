package de.gesellix.util

import groovy.json.JsonBuilder

class QueryUtil {

  def applyDefaults(Map query, defaults) {
    defaults.each { k, v ->
      if (!query.containsKey(k)) {
        query[k] = v
      }
    }
  }

  def jsonEncodeFilters(Map query) {
    jsonEncodeQueryParameter(query, "filters")
  }

  def jsonEncodeBuildargs(Map query) {
    jsonEncodeQueryParameter(query, "buildargs")
  }

  def jsonEncodeQueryParameter(Map query, String parameterName) {
    query.each { k, v ->
      if (k == parameterName) {
        query[k] = new JsonBuilder(v).toString()
      }
    }
  }
}
