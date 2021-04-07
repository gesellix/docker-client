package de.gesellix.docker.client.builder

import de.gesellix.util.IOUtils
import org.apache.commons.lang3.SystemUtils
import spock.lang.Requires
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

  def "handles trailing slashes in exclude patterns"() {
    given:
    def baseDir = IOUtils.getResource("/dockerignore_subdirs").file
    def base = new File(baseDir)
    when:
    def collectedFiles = new DockerignoreFileFilter(base, []).collectFiles(base)
    then:
    collectedFiles.sort() == [new File("${baseDir}/keepme/a-file-to-be-kept.txt"),
                              new File("${baseDir}/keepme/subdir/keep-me.txt")].sort()
  }

  @Requires({ SystemUtils.IS_OS_WINDOWS })
  def "handles trailing backslashes in patterns (windows only)"() {
    given:
    def baseDir = IOUtils.getResource("/dockerignore_windows").file
    def base = new File(baseDir)
    when:
    def collectedFiles = new DockerignoreFileFilter(base, []).collectFiles(base)
    then:
    collectedFiles == [new File("${baseDir}/keepme/to-be-kept.txt")]
  }
}
