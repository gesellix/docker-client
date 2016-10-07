package de.gesellix.docker.client

class TestimageConstants {

    final String imageRepo
    final String imageTag
    final String imageName
    final String imageDigest

    static TestimageConstants CONSTANTS = new TestimageConstants()

    TestimageConstants() {
        imageRepo = "gesellix/docker-client-testimage"
        imageTag = LocalDocker.isNativeWindows() ? "os-windows" : "os-linux"
        imageName = "$imageRepo:$imageTag"
        imageDigest = LocalDocker.isNamedPipe() ? "todo" : "sha256:a766bfd819b1116a6172c2a7a11a9e90f25222c00bdd740a5ad2575b000a7339"
    }
}
