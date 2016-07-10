package de.gesellix.docker.client

import groovy.util.logging.Slf4j

import static groovy.io.FileType.FILES

@Slf4j
class DockerignoreFileFilter {

    GlobsMatcher globsMatcher

    DockerignoreFileFilter(File base, additionalExcludes = []) {
        def dockerignore = getDockerignorePatterns(base)
        dockerignore += ".dockerignore"
        additionalExcludes.each {
            dockerignore += it
        }
        dockerignore = relativize(dockerignore as Collection, base)
        log.debug "base: ${base.absolutePath}"
        log.debug "dockerignore: ${dockerignore}"
        globsMatcher = new GlobsMatcher(dockerignore)
    }

    def getDockerignorePatterns(File base) {
        def dockerignoreFile = base.listFiles().find {
            def relativeFileName = relativize(base, it)
            return ".dockerignore" == relativeFileName
        }
        dockerignoreFile ? IOUtils.toString(new FileInputStream(dockerignoreFile as File)).split("[\r\n]+") : []
    }

    def relativize(Collection<String> dockerignores, File base) {
        dockerignores.collect { dockerignore ->
            new File(dockerignore).isAbsolute() ? relativize(base, new File(dockerignore)) : dockerignore
        }
    }

    def relativize(File base, File absolute) {
        return base.absoluteFile.toPath().relativize(absolute.absoluteFile.toPath()).toString()
    }

    def collectFiles(File base) {
        def ignoredDirs = []
        base.eachDirMatch(
                { globsMatcher.matches(base, new File("${base}${File.separator}${it}")) },
                { ignoredDirs << it })

        def files = []

        base.eachFileRecurse FILES, { File file ->
            def parentDirIgnored = ignoredDirs.find { File ignoredDir ->
                file.toPath().startsWith(ignoredDir.toPath())
            }

            if (!parentDirIgnored && !globsMatcher.matches(base, file)) {
                files << file
            }
        }

        return files
    }
}
