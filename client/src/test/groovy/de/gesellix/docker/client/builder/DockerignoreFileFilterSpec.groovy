package de.gesellix.docker.client.builder

import de.gesellix.docker.client.util.IOUtils
import spock.lang.Specification

class DockerignoreFileFilterSpec extends Specification {

    def "collects desired files"() {
        given:
        def baseDir = IOUtils.getResource("/dockerignore").file
        def base = new File(baseDir)
        when:
        def collectedFiles = new DockerignoreFileFilter(base, []).collectFiles(base)
        then:
        collectedFiles.sort() == [new File("${baseDir}/ignorefolder/keepme.txt"),
                                  new File("${baseDir}/subfolder/content.txt"),
                                  new File("${baseDir}/subfolder/subsubfolder/content.txt")].sort()
    }

    def "collects all non-dockerignored files"() {
        given:
        def baseDir = IOUtils.getResource("/dockerignore-all-but-some").file
        def base = new File(baseDir)
        when:
        def collectedFiles = new DockerignoreFileFilter(base, []).collectFiles(base)
        then:
        collectedFiles.sort() == [new File("${baseDir}/Dockerfile"),
                                  new File("${baseDir}/content.txt")].sort()
    }
}
