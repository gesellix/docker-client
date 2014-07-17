package de.gesellix.docker.client

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TarFileBuilder {

  private static Logger logger = LoggerFactory.getLogger(TarFileBuilder)

  def static archiveTarFilesRecursively(File base, File targetFile) throws IOException {
    def files = []
    base.eachFileRecurse {
      if (it != targetFile) {
        files << it
      }
    }
    logger.debug "found ${files.size()} files in buildContext."
    archiveTarFiles(base, files, targetFile)
  }

  def static archiveTarFiles(File base, Iterable<File> files, File targetFile) throws IOException {
    TarArchiveOutputStream tos = new TarArchiveOutputStream(new FileOutputStream(targetFile))
    try {
      tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
      def dockerignorePatterns = getDockerignorePatterns(base, files)
      for (File file : files) {
        def relativeFileName = relativize(base, file)
        addAsTarEntry(file, relativeFileName, tos)
//        if (dockerignorePatterns && ignoreFile(dockerignorePatterns, relativeFileName)) {
//          logger.debug "ignore file: ${relativeFileName}"
//        }
//        else {
//          logger.debug "add file: ${relativeFileName}"
//          addAsTarEntry(file, relativeFileName, tos)
//        }
      }
    }
    finally {
      tos.close()
    }
  }

  private static void addAsTarEntry(File file, String relativeFileName, TarArchiveOutputStream tos) {
    TarArchiveEntry tarEntry = new TarArchiveEntry(file)
    tarEntry.setName(relativeFileName)

    tos.putArchiveEntry(tarEntry)

    if (!file.isDirectory()) {
      copyFile(file, tos)
    }
    tos.closeArchiveEntry()
  }

  static ignoreFile(patterns, String relativeFileName) {
    def shallBeIgnored = false
    patterns.each { pattern ->
      logger.trace "check $relativeFileName on pattern '$pattern'"
      if (relativeFileName.matches("${pattern}.*")) {
        shallBeIgnored = true
      }
    }
    return shallBeIgnored
  }

  static getDockerignorePatterns(File base, Iterable<File> files) {
    def dockerignoreFile = files.find {
      def relativeFileName = relativize(base, it)
      return ".dockerignore" == relativeFileName
    }
    dockerignoreFile ? IOUtils.toString(new FileInputStream(dockerignoreFile)).split("\n") : []
  }

  def static String relativize(File base, File absolute) {
    String relative = base.toURI().relativize(absolute.toURI()).getPath()
    return relative
  }

  def static copyFile(File input, OutputStream output) throws IOException {
    final FileInputStream fis = new FileInputStream(input);
    try {
      IOUtils.copyLarge(fis, output);
    }
    finally {
      fis.close();
    }
  }
}
