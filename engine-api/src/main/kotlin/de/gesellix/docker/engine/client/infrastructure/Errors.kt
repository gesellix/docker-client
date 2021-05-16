package de.gesellix.docker.engine.client.infrastructure

open class ClientException(message: String? = null, val statusCode: Int = -1, val response: Response? = null) : RuntimeException(message) {

  companion object {

    private const val serialVersionUID: Long = 123L
  }

  override fun toString(): String {
    return (response as? ClientError<*>)?.body.toString()
  }
}

open class ServerException(message: String? = null, val statusCode: Int = -1, val response: Response? = null) : RuntimeException(message) {

  companion object {

    private const val serialVersionUID: Long = 456L
  }

  override fun toString(): String {
    return (response as? ServerError<*>)?.body.toString()
  }
}
