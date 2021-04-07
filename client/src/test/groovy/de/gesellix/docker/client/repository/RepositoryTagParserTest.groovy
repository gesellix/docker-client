package de.gesellix.docker.client.repository

import de.gesellix.docker.client.DockerClientException
import spock.lang.Specification
import spock.lang.Unroll

class RepositoryTagParserTest extends Specification {

  RepositoryTagParser parser

  def setup() {
    parser = new RepositoryTagParser()
  }

  @Unroll
  "parse repository tag #name to repo '#repo' and tag '#tag'"() {
    when:
    def result = parser.parseRepositoryTag(name)

    then:
    result.repo == repo
    and:
    result.tag == tag

    where:
    name                      || repo                  | tag
    "scratch"                 || "scratch"             | ""
    "root:tag"                || "root"                | "tag"
    "user/repo"               || "user/repo"           | ""
    "user/repo:tag"           || "user/repo"           | "tag"
    "url:5000/repo"           || "url:5000/repo"       | ""
    "url:5000/repo:tag"       || "url:5000/repo"       | "tag"
    "url:5000/user/image:tag" || "url:5000/user/image" | "tag"
  }

  def "shouldn't allow repository tag ending with a ':'"() {
    when:
    parser.parseRepositoryTag("scratch:")

    then:
    def exc = thrown(DockerClientException)
    exc.cause.message == "'scratch:' should not end with a ':'"
  }
}
