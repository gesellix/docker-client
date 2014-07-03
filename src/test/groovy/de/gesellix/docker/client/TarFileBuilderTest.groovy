package de.gesellix.docker.client

import org.apache.commons.io.IOUtils
import spock.lang.Specification

class TarFileBuilderTest extends Specification {

  def "test archiveTarFilesRecursively"() {
    TarFileBuilder.archiveTarFilesRecursively()
  }

  def "test archiveTarFiles"() {
    TarFileBuilder.archiveTarFiles()
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
