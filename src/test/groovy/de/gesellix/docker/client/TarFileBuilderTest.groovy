package de.gesellix.docker.client

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.io.IOUtils
import spock.lang.Specification

class TarFileBuilderTest extends Specification {

  def "test archiveTarFilesRecursively"() {
    given:
    def resource = getClass().getResource('/docker/Dockerfile')
    def inputDirectory = new File(resource.toURI()).parentFile
    def targetFile = File.createTempFile("buildContext", ".tar")
    targetFile.deleteOnExit()

    when:
    TarFileBuilder.archiveTarFilesRecursively(inputDirectory, targetFile)

    then:
    def collectedEntryNames = collectEntryNames(targetFile)
    collectedEntryNames.sort() == ["subdirectory/", "subdirectory/payload.txt", "Dockerfile"].sort()
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
    def ignoreFile = TarFileBuilder.ignoreFile([".git"], ".git/refs/remotes/")
    then:
    ignoreFile == true
  }

  def "test relativize"() {
    when:
    def relativized = TarFileBuilder.relativize(new File("./base/dir"), new File("./base/dir/with/sub/dir"))

    then:
    relativized == "with/sub/dir"
  }

  def "test copyFile"() {
    given:
    def resource = getClass().getResource('/docker/subdirectory/payload.txt')
    def inputFile = new File(resource.toURI())
    def outputStream = new ByteArrayOutputStream()

    when:
    TarFileBuilder.copyFile(inputFile, outputStream)

    then:
    new String(outputStream.toByteArray()) == IOUtils.toString(new FileInputStream(inputFile))
  }
}
