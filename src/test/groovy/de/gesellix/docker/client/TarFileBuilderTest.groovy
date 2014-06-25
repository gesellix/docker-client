package de.gesellix.docker.client

import spock.lang.Specification

class TarFileBuilderTest extends Specification {

  def "test copyFile"(){
    TarFileBuilder.copyFile()
  }

  def "test relativize"(){
    TarFileBuilder.relativize()
  }
}
