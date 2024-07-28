package de.gesellix.util;

import java.util.Map;

import com.squareup.moshi.Moshi;

public class QueryParameterEncoder {

  private final Moshi moshi = new Moshi.Builder().build();

  public void jsonEncodeQueryParameter(Map<String, Object> query, String parameterName) {
    query.forEach((k, v) -> {
      if (v != null && parameterName.equals(k)) {
        if (v instanceof Map) {
          query.put(k, moshi.adapter(Map.class).toJson((Map<?, ?>) v));
        } else {
          throw new UnsupportedOperationException(String.format("Only Maps are supported, but a $2%s has been found for key $1%s", k, v));
        }
      }
    });
  }
}
