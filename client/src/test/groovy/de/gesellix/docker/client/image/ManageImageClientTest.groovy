package de.gesellix.docker.client.image

import de.gesellix.docker.client.DockerResponseHandler
import de.gesellix.docker.engine.EngineClient
import de.gesellix.docker.engine.EngineResponseStatus
import groovy.json.JsonBuilder
import spock.lang.Specification

class ManageImageClientTest extends Specification {

    ManageImageClient service
    EngineClient httpClient = Mock(EngineClient)
    DockerResponseHandler responseHandler = Mock(DockerResponseHandler)

    def setup() {
        service = Spy(ManageImageClient, constructorArgs: [
                httpClient,
                responseHandler])
    }

    def "build with defaults"() {
        def buildContext = new ByteArrayInputStream([42] as byte[])

        when:
        def imageId = service.build(buildContext)

        then:
        1 * httpClient.post([path              : "/build",
                             query             : ["rm": true],
                             body              : buildContext,
                             requestContentType: "application/octet-stream",
                             async             : false]) >> [content: [[stream: "Successfully built foo"],
                                                                       [aux: [ID: "sha256:23455"]]]]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker build failed"
        }
        and:
        imageId == "sha256:23455"
    }

    def "build with query"() {
        def buildContext = new ByteArrayInputStream([42] as byte[])
        def query = ["rm": false]

        when:
        def imageId = service.build(buildContext, query)

        then:
        1 * httpClient.post([path              : "/build",
                             query             : ["rm": false],
                             body              : buildContext,
                             requestContentType: "application/octet-stream",
                             async             : false]) >> [content: [[stream: "Successfully built bar"],
                                                                       [aux: [ID: "sha256:23455"]]]]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker build failed"
        }
        and:
        imageId == "sha256:23455"
    }

    def "build with auth"() {
        def buildContext = new ByteArrayInputStream([42] as byte[])
        def query = ["rm": false]
        def buildOptions = [ EncodedRegistryConfig : "NDI="]

        when:
        def imageId = service.build(buildContext, query, null, buildOptions)

        then:
        1 * httpClient.post([path              : "/build",
                             query             : ["rm": false],
                             body              : buildContext,
                             headers           : ["X-Registry-Config" : "NDI="],
                             requestContentType: "application/octet-stream",
                             async             : false]) >> [content: [[stream: "Successfully built bar"],
                                                                       [aux: [ID: "sha256:23455"]]]]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker build failed"
        }
        and:
        imageId == "sha256:23455"
    }

    def "build with BuildConfig"() {
        def buildContext = new ByteArrayInputStream([42] as byte[])
        def query = ["rm": false]
        def buildOptions = [ EncodedRegistryConfig : "NDI="]
        BuildConfig config = new BuildConfig()
        config.buildContext = buildContext
        config.query = query
        config.options = buildOptions

        when:
        def imageId = service.build(config)

        then:
        1 * httpClient.post([path              : "/build",
                             query             : ["rm": false],
                             body              : buildContext,
                             headers           : ["X-Registry-Config" : "NDI="],
                             requestContentType: "application/octet-stream",
                             async             : false]) >> [content: [[stream: "Successfully built bar"],
                                                                       [aux: [ID: "sha256:23455"]]]]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker build failed"
        }
        and:
        imageId == "sha256:23455"
    }

    def "build with duplicates"() {
        def buildContext = new ByteArrayInputStream([42] as byte[])

        when:
        def imageId = service.build(buildContext)

        then:
        1 * httpClient.post([path              : "/build",
                             query             : ["rm": true],
                             body              : buildContext,
                             requestContentType: "application/octet-stream",
                             async             : false]) >> [content: [
                ["stream": "Successfully built arrow tornado\n"],
                ["stream": "Successfully built 5d45b2048a3e\n"]
        ]]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker build failed"
        }
        and:
        imageId == "5d45b2048a3e"
    }

    def "tag with defaults"() {
        when:
        service.tag("an-image", "registry:port/username/image-name:a-tag")

        then:
        1 * httpClient.post([path : "/images/an-image/tag",
                             query: [repo: "registry:port/username/image-name",
                                     tag : "a-tag"]])
    }

    def "push with defaults"() {
        when:
        service.push("an-image")

        then:
        1 * httpClient.post([path   : "/images/an-image/push",
                             query  : [tag: ""],
                             headers: ["X-Registry-Auth": "."]]) >> [status: [success: true]]
    }

    def "push with auth"() {
        when:
        service.push("an-image:a-tag", "some-base64-encoded-auth")

        then:
        1 * httpClient.post([path   : "/images/an-image/push",
                             query  : [tag: "a-tag"],
                             headers: ["X-Registry-Auth": "some-base64-encoded-auth"]]) >> [status: [success: true]]
    }

    def "push with registry"() {
        when:
        service.push("an-image", ".", "registry:port")

        then:
        1 * httpClient.post([path : "/images/an-image/tag",
                             query: [repo: "registry:port/an-image",
                                     tag : ""]])
        then:
        1 * httpClient.post([path   : "/images/registry:port/an-image/push",
                             query  : [tag: ""],
                             headers: ["X-Registry-Auth": "."]]) >> [status: [success: true]]
    }

    def "pull with defaults"() {
        given:
        service.images([:]) >> [content: [:]]

        when:
        service.pull("an-image")

        then:
        1 * httpClient.post([path   : "/images/create",
                             query  : [fromImage: "an-image",
                                       tag      : ""],
                             headers: ["X-Registry-Auth": "."]]) >> [content: [[id: "image-id"]]]
        and:
        1 * responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker images create failed"
        }
    }

    def "pull with tag"() {
        given:
        service.images([:]) >> [content: [:]]

        when:
        service.pull("an-image", "a-tag")

        then:
        1 * httpClient.post([path   : "/images/create",
                             query  : [fromImage: "an-image",
                                       tag      : "a-tag"],
                             headers: ["X-Registry-Auth": "."]]) >> [content: [[id: "image-id"]]]
        and:
        1 * responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker images create failed"
        }
    }

    def "pull with registry"() {
        given:
        service.images([:]) >> [content: [:]]

        when:
        service.pull("an-image", "", ".", "registry:port")

        then:
        1 * httpClient.post([path   : "/images/create",
                             query  : [fromImage: "registry:port/an-image",
                                       tag      : ""],
                             headers: ["X-Registry-Auth": "."]]) >> [content: [[id: "image-id"]]]
        and:
        1 * responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker images create failed"
        }
    }

    def "pull with auth"() {
        given:
        service.images([:]) >> [content: [:]]

        when:
        service.pull("an-image", "", "some-base64-encoded-auth", "registry:port")

        then:
        1 * httpClient.post([path   : "/images/create",
                             query  : [fromImage: "registry:port/an-image",
                                       tag      : ""],
                             headers: ["X-Registry-Auth": "some-base64-encoded-auth"]]) >> [content: [[id: "image-id"]]]
        and:
        1 * responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker images create failed"
        }
    }

    def "import from url"() {
        given:
        def importUrl = getClass().getResource('importUrl/import-from-url.tar')

        when:
        def imageId = service.importUrl(importUrl.toString(), "imported-from-url", "foo")

        then:
        1 * httpClient.post([path : "/images/create",
                             query: [fromSrc: importUrl.toString(),
                                     repo   : "imported-from-url",
                                     tag    : "foo"]]) >> [content: [[status: "image-id"]]]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker import from url failed"
        }
        and:
        imageId == "image-id"
    }

    def "import from stream"() {
        given:
        def archive = getClass().getResourceAsStream('importUrl/import-from-url.tar')

        when:
        def imageId = service.importStream(archive, "imported-from-url", "foo")

        then:
        1 * httpClient.post([path              : "/images/create",
                             body              : archive,
                             requestContentType: "application/x-tar",
                             query             : [fromSrc: '-',
                                                  repo   : "imported-from-url",
                                                  tag    : "foo"]]) >> [content: [status: "image-id"]]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker import from stream failed"
        }
        and:
        imageId == "image-id"
    }

    def "save one repository"() {
        given:
        def tarStream = new ByteArrayInputStream("tar".bytes)

        when:
        def response = service.save("image:tag")

        then:
        1 * httpClient.get([path: "/images/image:tag/get"]) >> [status: [success: true],
                                                                stream: tarStream]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker save failed"
        }
        and:
        response.stream == tarStream
    }

    def "save multiple repositories"() {
        given:
        def tarStream = new ByteArrayInputStream("tar".bytes)

        when:
        def response = service.save("image:tag1", "an-id")

        then:
        1 * httpClient.get([path : "/images/get",
                            query: [names: ["image:tag1", "an-id"]]]) >> [status: [success: true],
                                                                          stream: tarStream]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker save failed"
        }
        and:
        response.stream == tarStream
    }

    def "load"() {
        given:
        def archive = getClass().getResourceAsStream('importUrl/import-from-url.tar')

        when:
        def response = service.load(archive)

        then:
        1 * httpClient.post([path              : "/images/load",
                             body              : archive,
                             requestContentType: "application/x-tar"]) >> [status: [success: true]]
        and:
        responseHandler.ensureSuccessfulResponse(*_) >> { arguments ->
            assert arguments[1]?.message == "docker load failed"
        }
        and:
        response.status == new EngineResponseStatus(success: true)
    }

    def "inspect image"() {
        when:
        service.inspectImage("an-image")

        then:
        1 * httpClient.get([path: "/images/an-image/json"]) >> [status : [success: true],
                                                                content: [:]]
    }

    def "history"() {
        when:
        service.history("an-image")

        then:
        1 * httpClient.get([path: "/images/an-image/history"])
    }

    def "images with defaults"() {
        when:
        service.images()

        then:
        1 * httpClient.get([path : "/images/json",
                            query: [all: false]]) >> [status: [success: true]]
    }

    def "images with query"() {
        given:
        def filters = [dangling: ["true"]]
        def expectedFilterValue = new JsonBuilder(filters).toString()
        def query = [all    : true,
                     filters: filters]

        when:
        service.images(query)

        then:
        1 * httpClient.get([path : "/images/json",
                            query: [all    : true,
                                    filters: expectedFilterValue]]) >> [status: [success: true]]
    }

    def "findImageId by image name"() {
        given:
        service.images([:]) >> [content: [[RepoTags: ['anImage:latest'],
                                           Id      : 'the-id']]]

        expect:
        service.findImageId('anImage') == 'the-id'
    }

    def "findImageId with missing image"() {
        given:
        service.images([:]) >> [content: []]

        expect:
        service.findImageId('anImage') == 'anImage:latest'
    }

    def "findImageId by digest"() {
        given:
        service.images(_) >> [content: [[RepoDigests: ['anImage@sha256:4711'],
                                         Id         : 'the-id']]]

        expect:
        service.findImageId('anImage@sha256:4711') == 'the-id'
    }

    def "rmi image"() {
        when:
        service.rmi("an-image")

        then:
        1 * httpClient.delete([path: "/images/an-image"])
    }

    def "pruneImages removes unused images"() {
        given:
        def filters = [dangling: true]
        def expectedFilterValue = new JsonBuilder(filters).toString()

        when:
        service.pruneImages([filters: filters])

        then:
        1 * httpClient.post([path : "/images/prune",
                             query: [filters: expectedFilterValue]]) >> [status: [success: true]]
    }
}
