package de.gesellix.docker.engine.client.infrastructure

import de.gesellix.docker.engine.RequestMethod
import java.lang.reflect.Type

/**
 * Defines a config object for a given request.
 * NOTE: This object doesn't include 'body' because it
 *       allows for caching of the constructed object
 *       for many request definitions.
 * NOTE: Headers is a Map<String,String> because rfc2616 defines
 *       multi-valued headers as csv-only.
 */
data class RequestConfig(
  val method: RequestMethod,
  val path: String,
  val headers: MutableMap<String, String> = mutableMapOf(),
  val query: MutableMap<String, List<String>> = mutableMapOf(),
  val body: Any? = null,
  val elementType: Type? = null
)
