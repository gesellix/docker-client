package de.gesellix.docker.engine.client.infrastructure

data class Element<T>(val type: ResponseType, val data: T? = null)
