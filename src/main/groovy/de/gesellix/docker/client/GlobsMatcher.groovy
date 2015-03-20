package de.gesellix.docker.client

import java.nio.file.FileSystems
import java.nio.file.PathMatcher

class GlobsMatcher {

  Map<String, PathMatcher> matchers

  GlobsMatcher(globs) {
    def fileSystem = FileSystems.getDefault()
    matchers = globs.collectEntries {
      ["glob:$it": fileSystem.getPathMatcher("glob:$it")]
    }
  }

  def matches(File base, File path) {
    def relativePath = base.absoluteFile.toPath().relativize(path.absoluteFile.toPath())
    def result = matchers.find { key, matcher ->
      matcher.matches(relativePath) ? key : null
    }
    return result
  }
}
