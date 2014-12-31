package de.gesellix.docker.client

import java.nio.file.FileSystems

class GlobsMatcher {
  def matchers

  GlobsMatcher(globs) {
    def fileSystem = FileSystems.getDefault()
    matchers = globs.collect {
      fileSystem.getPathMatcher("glob:$it")
    }
  }

  def matches(File base, File path) {
    def relativePath = base.toPath().relativize(path.toPath())
    def result = matchers.find {
      it.matches(relativePath)
    }
    return result
  }
}
