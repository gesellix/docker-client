package de.gesellix.docker.client

import groovy.transform.ToString

@ToString(includeNames = true)
class DockerResponse {

  def status
  def headers
  def contentType
  def mimeType
  def contentLength
  def stream
  def content
}
