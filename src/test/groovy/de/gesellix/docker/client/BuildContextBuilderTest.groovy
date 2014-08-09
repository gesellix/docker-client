package de.gesellix.docker.client

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import spock.lang.Specification

class BuildContextBuilderTest extends Specification {

  def "test archiveTarFilesRecursively"() {
    given:
    def inputDirectory = new ResourceReader().getClasspathResourceAsFile('/docker/Dockerfile').parentFile
    def targetFile = File.createTempFile("buildContext", ".tar")
    targetFile.deleteOnExit()

    when:
    BuildContextBuilder.archiveTarFilesRecursively(inputDirectory, targetFile)

    then:
    def collectedEntryNames = collectEntryNames(targetFile)
    collectedEntryNames.sort() == ["subdirectory/", "subdirectory/payload.txt", "Dockerfile"].sort()
  }

  def "test archiveTarFilesRecursively excludes targetFile"() {
    given:
    def inputDirectory = new ResourceReader().getClasspathResourceAsFile('/docker/Dockerfile').parentFile
    def targetFile = new File(inputDirectory, "buildContext.tar")
    targetFile.createNewFile()
    targetFile.deleteOnExit()

    when:
    BuildContextBuilder.archiveTarFilesRecursively(inputDirectory, targetFile)

    then:
    def collectedEntryNames = collectEntryNames(targetFile)
    collectedEntryNames.sort() == ["subdirectory/", "subdirectory/payload.txt", "Dockerfile"].sort()

    // TODO cannot be deleted while the Gradle daemon is running?
//    cleanup:
//    Files.delete(targetFile.toPath())
//    println targetFile.delete()
  }

  def collectEntryNames(File tarArchive) {
    def collectedEntryNames = []
    def tarArchiveInputStream = new TarArchiveInputStream(new FileInputStream(tarArchive))

    def entry
    while (entry = tarArchiveInputStream.nextTarEntry) {
      collectedEntryNames << entry.name
    }
    collectedEntryNames
  }

  def "test ignoreFile"() {
    when:
    def ignoreFile = BuildContextBuilder.ignoreFile([".git"], ".git/refs/remotes/")
    then:
    ignoreFile == true
  }

  def "test relativize"() {
    when:
    def relativized = BuildContextBuilder.relativize(new File("./base/dir"), new File("./base/dir/with/sub/dir"))

    then:
    relativized == "with/sub/dir"
  }

  def "test copyFile"() {
    given:
    def inputFile = new ResourceReader().getClasspathResourceAsFile('/docker/subdirectory/payload.txt')
    def outputStream = new ByteArrayOutputStream()

    when:
    BuildContextBuilder.copyFile(inputFile, outputStream)

    then:
    new String(outputStream.toByteArray()) == IOUtils.toString(new FileInputStream(inputFile))
  }
}
