package de.gesellix.docker.client.builder

import groovy.util.logging.Slf4j

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher

@Slf4j
class GlobsMatcher {

    File base
    List<String> globs
    List<Matcher> matchers

    GlobsMatcher(File base, List<String> globs) {
        this.base = base
        this.globs = globs
    }

    void initMatchers() {
        if (this.matchers == null) {
            def fileSystem = FileSystems.getDefault()
            this.matchers = globs.collectMany {
                if (it.endsWith("/")) {
                    return [new Matcher(fileSystem, it.replaceAll("/\$", "")),
                            new Matcher(fileSystem, it.replaceAll("/\$", "/**"))]
                }
                else {
                    return [new Matcher(fileSystem, it)]
                }
            }.reverse()
            matchers.each {
                log.debug("pattern: ${it.pattern}")
            }
        }
    }

    boolean matches(File path) {
        initMatchers()

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

        static String separator = File.separatorChar

        Matcher(FileSystem fileSystem, String pattern) {
            // According to https://docs.docker.com/engine/reference/builder/#dockerignore-file
            // and https://golang.org/pkg/path/filepath/#Clean we clean paths
            // by removing trailing slashes and also by replacing slashes with the path separator.
            this.pattern = pattern.replaceAll("/", "\\${separator}")
                    .split("\\${separator}")
                    .join("\\${separator}")
            String negation = "!"
            this.negate = pattern.startsWith(negation)
            if (this.negate) {
                String invertedPattern = this.pattern.substring(negation.length())
                this.matcher = createGlob(fileSystem, invertedPattern)
            }
            else {
                this.matcher = createGlob(fileSystem, this.pattern)
            }
        }

        static PathMatcher createGlob(FileSystem fileSystem, String glob) {
            return fileSystem.getPathMatcher("glob:${glob}")
        }

        @Override
        boolean matches(Path path) {
            return matcher.matches(path)
        }
    }
}
