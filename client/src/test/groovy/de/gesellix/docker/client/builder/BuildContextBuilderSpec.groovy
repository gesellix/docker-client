package de.gesellix.docker.client.builder

import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.util.IOUtils
import de.gesellix.docker.testutil.ResourceReader
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import spock.lang.Specification

import java.util.zip.GZIPInputStream

class BuildContextBuilderSpec extends Specification {

    def "test archiveTarFilesRecursively"() {
        given:
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile('/docker/Dockerfile', DockerClient).parentFile
        def targetFile = File.createTempFile("buildContext", ".tar")
        targetFile.deleteOnExit()

        when:
        BuildContextBuilder.archiveTarFilesRecursively(inputDirectory, targetFile)

        then:
        def collectedEntryNames = collectEntryNames(targetFile)
        collectedEntryNames.sort() == ["subdirectory/payload.txt", "Dockerfile", "script.sh"].sort()
    }

    def "test archiveTarFilesRecursively keeps executable flag"() {
        given:
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile('/docker/Dockerfile', DockerClient).parentFile
        def targetFile = File.createTempFile("buildContext", ".tar")
        targetFile.deleteOnExit()

        when:
        BuildContextBuilder.archiveTarFilesRecursively(inputDirectory, targetFile)

        then:
        getFileMode(targetFile, "script.sh") == 0100755
    }

    def "test archiveTarFilesRecursively excludes targetFile when in same baseDir"() {
        given:
        def inputDirectory = new ResourceReader().getClasspathResourceAsFile('/docker/Dockerfile', DockerClient).parentFile
        def targetFile = new File(inputDirectory, "buildContext.tar")
        targetFile.createNewFile()
        targetFile.deleteOnExit()

        when:
        BuildContextBuilder.archiveTarFilesRecursively(inputDirectory, targetFile)

        then:
        def collectedEntryNames = collectEntryNames(targetFile)
        collectedEntryNames.sort() == ["subdirectory/payload.txt", "Dockerfile", "script.sh"].sort()

        // TODO cannot be deleted while the Gradle daemon is running?
//    cleanup:
//    Files.delete(targetFile.toPath())
//    println targetFile.delete()
    }

    def collectEntryNames(File tarArchive) {
        def collectedEntryNames = []
        def tarArchiveInputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(tarArchive)))

        def entry
        while (entry = tarArchiveInputStream.nextTarEntry) {
            collectedEntryNames << entry.name
        }
        collectedEntryNames
    }

    def getFileMode(File tarArchive, String filename) {
        def tarArchiveInputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(tarArchive)))

        def entry
        while (entry = tarArchiveInputStream.nextTarEntry) {
            if (entry.name == filename) {
                return entry.getMode()
            }
        }
        throw new FileNotFoundException(filename)
    }

    def "test relativize"() {
        when:
        def relativized = BuildContextBuilder.relativize(new File("./base/dir"), new File("./base/dir/with/sub/dir"))

        then:
        relativized == new File("with/sub/dir").toPath().toString()
    }

    def "test copyFile"() {
        given:
        def inputFile = new ResourceReader().getClasspathResourceAsFile('/docker/subdirectory/payload.txt', DockerClient)
        def outputStream = new ByteArrayOutputStream()

        when:
        BuildContextBuilder.copyFile(inputFile, outputStream)

        then:
        new String(outputStream.toByteArray()) == IOUtils.toString(new FileInputStream(inputFile))
    }
}
