package de.gesellix.docker.client.builder

import groovy.util.logging.Slf4j

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher

@Slf4j
class GlobsMatcher {

    File base
    List<Matcher> matchers

    GlobsMatcher(File base, List<String> globs) {
        this.base = base
        def fileSystem = FileSystems.getDefault()
        this.matchers = globs.collectMany {
            if (it.endsWith("/")) {
                [new Matcher(fileSystem, it.replaceAll("/\$", "")),
                 new Matcher(fileSystem, it.replaceAll("/\$", "/**"))]
            }
            else {
                [new Matcher(fileSystem, it)]
            }
        }.reverse()
        matchers.each {
            log.debug("pattern: ${it.pattern}")
        }
    }

    def matches(File path) {
        def relativePath = base.absoluteFile.toPath().relativize(path.absoluteFile.toPath())
        def match = matchers.find {
            it.matches(relativePath)
        }
        if (!match && relativePath.parent) {
            match = matchers.find {
                it.matches(relativePath.parent)
            }
        }
        return match && !match.negate
    }

    static class Matcher implements PathMatcher {

        String pattern
        PathMatcher matcher
        boolean negate

        Matcher(fileSystem, String pattern) {
            this.negate = pattern.startsWith("!")
            this.pattern = pattern
            if (this.negate) {
                def invertedPattern = pattern.substring("!".length())
                this.matcher = fileSystem.getPathMatcher("glob:${invertedPattern}")
            }
            else {
                this.matcher = fileSystem.getPathMatcher("glob:${pattern}")
            }
        }

        @Override
        boolean matches(Path path) {
            return matcher.matches(path)
        }
    }
}
