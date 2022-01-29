package de.gesellix.util

import com.squareup.moshi.Moshi

class QueryUtil {

  private Moshi moshi = new Moshi.Builder().build()

  void applyDefaults(Map<String, ?> query, Map<String, ?> defaults) {
    defaults.each { String k, Object v ->
      if (!query.containsKey(k)) {
        query.put(k, v)
      }
    }
  }

  void jsonEncodeQueryParameter(Map<String, ?> query, String parameterName) {
    query.each { String k, Object v ->
      if (v != null && k == parameterName) {
        if (v instanceof Map) {
          query.put(k, moshi.adapter(Map).toJson(v))
        }
        else {
          throw new UnsupportedOperationException("Only Maps are supported, but a ${v.class} has been found for key $k")
        }
      }
    }
  }
}
