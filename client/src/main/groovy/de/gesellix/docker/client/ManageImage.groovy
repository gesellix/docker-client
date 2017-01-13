package de.gesellix.docker.client

interface ManageImage {

//    build       Build an image from a Dockerfile

    def buildWithLogs(InputStream buildContext)

    def buildWithLogs(InputStream buildContext, query)

    def buildWithLogs(InputStream buildContext, query, Timeout timeout)

    def build(InputStream buildContext)

    def build(InputStream buildContext, query)

    def build(InputStream buildContext, query, DockerAsyncCallback callback)

//    history     Show the history of an image

    def history(image)

//    import      Import the contents from a tarball to create a filesystem image

    def importUrl(url)

    def importUrl(url, repository)

    def importUrl(url, repository, tag)

    def importStream(stream)

    def importStream(stream, repository)

    def importStream(stream, repository, tag)

//    inspect     Display detailed information on one or more images

    def inspectImage(image)

//    load        Load an image from a tar archive or STDIN

    def load(stream)

//    ls          List images

    def images()

    def images(query)

//    prune       Remove unused images

    def pruneImages()

    def pruneImages(query)

//    pull        Pull an image or a repository from a registry

    def pull(image)

    def pull(image, tag)

    def pull(image, tag, authBase64Encoded)

    def pull(image, tag, authBase64Encoded, registry)

//    push        Push an image or a repository to a registry

    def push(image)

    def push(image, authBase64Encoded)

    def push(image, authBase64Encoded, registry)

//    rm          Remove one or more images

    def rmi(image)

//    save        Save one or more images to a tar archive (streamed to STDOUT by default)

    def save(... images)

//    tag         Create a tag TARGET_IMAGE that refers to SOURCE_IMAGE

    def tag(image, repository)
}
