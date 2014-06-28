package de.gesellix.docker.client

import spock.lang.Specification

class TarFileBuilderTest extends Specification {

  def "test archiveTarFilesRecursively"() {
    TarFileBuilder.archiveTarFilesRecursively()
  }

  def "test archiveTarFiles"() {
    TarFileBuilder.archiveTarFiles()
  }

  def "test copyFile"(){
    TarFileBuilder.copyFile()
  }

  def "test relativize"(){
    when:
    def relativized = TarFileBuilder.relativize(new File("./base/dir"), new File("./base/dir/with/sub/dir"))

    then:
    relativized == "with/sub/dir"
  }
}
