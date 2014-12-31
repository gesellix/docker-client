package de.gesellix.docker.client

import groovy.io.FileType
import org.apache.commons.io.IOUtils

class DockerignoreFileFilter {

  def globsMatcher

  DockerignoreFileFilter(File base, additionalExcludes) {
    def dockerignore = getDockerignorePatterns(base)
    dockerignore += ".dockerignore"
    additionalExcludes.each {
      dockerignore += it
    }
    globsMatcher = new GlobsMatcher(dockerignore)
  }

  def getDockerignorePatterns(File base) {
    def dockerignoreFile = base.listFiles().find {
      def relativeFileName = relativize(base, it)
      return ".dockerignore" == relativeFileName
    }
    dockerignoreFile ? IOUtils.toString(new FileInputStream(dockerignoreFile as File)).split("[\r\n]+") : []
  }

  def relativize(File base, File absolute) {
    return base.toPath().relativize(absolute.toPath()).toString()
  }

  def collectFiles(File base) {
    def files = []

    base.eachFileRecurse(FileType.FILES, {
      if (!globsMatcher.matches(base, it)) {
        files << it
      }
    })
    return files
  }
}
