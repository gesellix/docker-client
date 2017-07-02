package de.gesellix.docker.client.distribution

import spock.lang.Specification
import spock.lang.Unroll

class ReferenceParserTest extends Specification {

    ReferenceParser parser

    def setup() {
        parser = new ReferenceParser()
    }

    @Unroll
    def "parse '#s'"() {
        when:
        def reference = parser.parse(s)
        then:
        reference == r
        where:
        s                                                                                                                                                                           || r
        "docker.io/library/alpine:edge"                                                                                                                                             || [repo: [domain: "docker.io", path: "library/alpine"], tag: "edge"]
        "test_com"                                                                                                                                                                  || [domain: "", path: "test_com"]
        "test.com:tag"                                                                                                                                                              || [repo: [domain: "", path: "test.com"], tag: "tag"]
        "test.com:5000"                                                                                                                                                             || [repo: [domain: "", path: "test.com"], tag: "5000"]
        "test_com/foo"                                                                                                                                                              || [domain: "", path: "test_com/foo"]
        "test.com/foo"                                                                                                                                                              || [domain: "test.com", path: "foo"]
        "test.com/repo:tag"                                                                                                                                                         || [repo: [domain: "test.com", path: "repo"], tag: "tag"]
        "test:5000/repo"                                                                                                                                                            || [domain: "test:5000", path: "repo"]
        "test:8080/foo"                                                                                                                                                             || [domain: "test:8080", path: "foo"]
        "test.com:8080/foo"                                                                                                                                                         || [domain: "test.com:8080", path: "foo"]
        "test-com:8080/foo"                                                                                                                                                         || [domain: "test-com:8080", path: "foo"]
        "test:5000/repo:tag"                                                                                                                                                        || [repo: [domain: "test:5000", path: "repo"], tag: "tag"]
        "lowercase:Uppercase"                                                                                                                                                       || [repo: [domain: "", path: "lowercase"], tag: "Uppercase"]
        "foo_bar.com:8080"                                                                                                                                                          || [repo: [domain: "", path: "foo_bar.com"], tag: "8080"]
        "foo/foo_bar.com:8080"                                                                                                                                                      || [repo: [domain: "foo", path: "foo_bar.com"], tag: "8080"]
        "sub-dom1.foo.com/bar/baz/quux"                                                                                                                                             || [domain: "sub-dom1.foo.com", path: "bar/baz/quux"]
        "sub-dom1.foo.com/bar/baz/quux:some-long-tag"                                                                                                                               || [repo: [domain: "sub-dom1.foo.com", path: "bar/baz/quux"], tag: "some-long-tag"]
        "b.gcr.io/test.example.com/my-app:test.example.com"                                                                                                                         || [repo: [domain: "b.gcr.io", path: "test.example.com/my-app"], tag: "test.example.com"]
        // ☃.com in punycode
        "xn--n3h.com:18080/foo"                                                                                                                                                     || [domain: "xn--n3h.com:18080", path: "foo"]
        "xn--n3h.com/myimage:xn--n3h.com"                                                                                                                                           || [repo: [domain: "xn--n3h.com", path: "myimage"], tag: "xn--n3h.com"]
        // 🐳.com in punycode
        "xn--7o8h.com/myimage:xn--7o8h.com@sha512:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" || [repo: [domain: "xn--7o8h.com", path: "myimage"], tag: "xn--7o8h.com", digest: "sha512:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"]
        "test:5000/repo@sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"                                                                                    || [repo: [domain: "test:5000", path: "repo"], digest: "sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"]
        "test:5000/repo:tag@sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"                                                                                || [repo: [domain: "test:5000", path: "repo"], tag: "tag", digest: "sha256:ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"]
    }
}
