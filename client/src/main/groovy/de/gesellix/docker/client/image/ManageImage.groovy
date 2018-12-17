package de.gesellix.docker.client.image

import de.gesellix.docker.client.DockerAsyncCallback
import de.gesellix.docker.client.Timeout
import de.gesellix.docker.engine.EngineResponse

interface ManageImage {

//    build       Build an image from a Dockerfile

    BuildResult buildWithLogs(InputStream buildContext)

    BuildResult buildWithLogs(InputStream buildContext, BuildConfig config)

    /**
     * @deprecated use buildWithLogs(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
     * @see #buildWithLogs(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
     */
    @Deprecated
    def buildWithLogs(InputStream buildContext, Map query)

    /**
     * @deprecated use buildWithLogs(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
     * @see #buildWithLogs(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
     */
    @Deprecated
    def buildWithLogs(InputStream buildContext, Map query, Timeout timeout)

    BuildResult build(InputStream buildContext)

    BuildResult build(InputStream buildContext, BuildConfig config)

    /**
     * @deprecated use build(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
     * @see #build(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
     */
    @Deprecated
    def build(InputStream buildContext, Map query)

    /**
     * @deprecated use build(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
     * @see #build(java.io.InputStream, de.gesellix.docker.client.image.BuildConfig)
     */
    @Deprecated
    def build(InputStream buildContext, Map query, DockerAsyncCallback callback)

//    history     Show the history of an image

    EngineResponse history(image)

//    import      Import the contents from a tarball to create a filesystem image

    def importUrl(url)

    def importUrl(url, repository)

    def importUrl(url, repository, tag)

    String importStream(stream)

    String importStream(stream, repository)

    String importStream(stream, repository, tag)

//    inspect     Display detailed information on one or more images

    EngineResponse inspectImage(image)

//    load        Load an image from a tar archive or STDIN

    EngineResponse load(stream)

//    ls          List images

    EngineResponse images()

    EngineResponse images(query)

//    prune       Remove unused images

    EngineResponse pruneImages()

    EngineResponse pruneImages(query)

//    create      Create an image by either pulling it from a registry or importing it

    EngineResponse create(Map query)

    EngineResponse create(Map query, Map createOptions)

//    pull        Pull an image or a repository from a registry

    /**
     * @deprecated please use #create(query, createOptions)
     * @see #create(Map, Map)
     */
    @Deprecated
    String pull(image)

    /**
     * @deprecated please use #create(query, createOptions)
     * @see #create(Map, Map)
     */
    @Deprecated
    String pull(image, String tag)

    /**
     * @deprecated please use #create(query, createOptions)
     * @see #create(Map, Map)
     */
    @Deprecated
    String pull(image, String tag, String authBase64Encoded)

    /**
     * @deprecated please use #create(query, createOptions)
     * @see #create(Map, Map)
     */
    @Deprecated
    String pull(image, String tag, String authBase64Encoded, String registry)

//    push        Push an image or a repository to a registry

    EngineResponse push(String image)

    EngineResponse push(String image, String authBase64Encoded)

    EngineResponse push(String image, String authBase64Encoded, String registry)

//    rm          Remove one or more images

    EngineResponse rmi(String image)

//    save        Save one or more images to a tar archive (streamed to STDOUT by default)

    EngineResponse save(... images)

//    tag         Create a tag TARGET_IMAGE that refers to SOURCE_IMAGE

    EngineResponse tag(image, repository)

    String findImageId(imageName)

    String findImageId(imageName, tag)
}
