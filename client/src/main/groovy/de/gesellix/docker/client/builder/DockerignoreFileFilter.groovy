package de.gesellix.docker.client.builder

import de.gesellix.util.IOUtils
import groovy.util.logging.Slf4j

import static groovy.io.FileType.FILES

@Slf4j
class DockerignoreFileFilter {

  GlobsMatcher globsMatcher

  DockerignoreFileFilter(File base, List<String> additionalExcludes = []) {
    def dockerignore = getDockerignorePatterns(base)
    dockerignore += ".dockerignore"
    for (it in additionalExcludes) {
      dockerignore += it
    }
    try {
      dockerignore = relativize(dockerignore as Collection, base)
    }
    catch (IllegalArgumentException e) {
      log.error("base: ${base.absolutePath}, dockerignore: ${dockerignore}", e)
      throw e
    }
    log.debug "base: ${base.absolutePath}"
    log.debug "dockerignore: ${dockerignore}"
    globsMatcher = new GlobsMatcher(base, dockerignore)
  }

  def getDockerignorePatterns(File base) {
    def dockerignoreFile = base.listFiles().find {
      String relativeFileName = relativize(base, it)
      return ".dockerignore" == relativeFileName
    }
    dockerignoreFile ? IOUtils.toString(new FileInputStream(dockerignoreFile as File)).split("[\r\n]+") : []
  }

  def relativize(Collection<String> dockerignores, File base) {
    dockerignores.collect { String dockerignore ->
      new File(dockerignore).isAbsolute() ? relativize(base, new File(dockerignore)) : dockerignore
    }
  }

  String relativize(File base, File absolute) {
    def basePath = base.absoluteFile.toPath()
    def otherPath = absolute.absoluteFile.toPath()
    if (basePath.root != otherPath.root) {
      // Can occur on Windows, when
      // - java temp directory is under C:/
      // - project directory is under D:/
      return otherPath.toString()
    }
    return basePath.relativize(otherPath).toString()
  }

  def collectFiles(File base) {
    def files = []
    base.eachFileRecurse FILES, { File file ->
      if (!globsMatcher.matches(file)) {
        files << file
      }
    }
    log.debug "filtered list of files: ${files}"
    return files
  }
}
