package de.gesellix.docker.client

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files

class BuildContextBuilder {

  private static Logger logger = LoggerFactory.getLogger(BuildContextBuilder)

  def static archiveTarFilesRecursively(File base, File targetFile) throws IOException {
    def filenames = new DockerignoreFileFilter(base, [targetFile.name]).collectFiles(base)
    logger.debug "found ${filenames.size()} files in buildContext."
    archiveTarFiles(base, filenames, targetFile)
  }

  def static archiveTarFiles(File base, filenames, File targetFile) throws IOException {
    TarArchiveOutputStream tos = new TarArchiveOutputStream(new FileOutputStream(targetFile))
    try {
      tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
      for (String filename : filenames) {
        def relativeFileName = relativize(base, new File(filename))
        addAsTarEntry(new File(filename), relativeFileName, tos)
      }
    }
    finally {
      tos.close()
    }
  }

  static addAsTarEntry(File file, String relativeFileName, TarArchiveOutputStream tos) {
    TarArchiveEntry tarEntry = new TarArchiveEntry(file)
    tarEntry.setName(relativeFileName)

    if (!file.isDirectory()) {
      if (Files.isExecutable(file.toPath())) {
        tarEntry.setMode(tarEntry.getMode() | 0755)
      }
    }

    tos.putArchiveEntry(tarEntry)

    if (!file.isDirectory()) {
      copyFile(file, tos)
    }
    tos.closeArchiveEntry()
  }

  def static String relativize(File base, File absolute) {
    return base.toPath().relativize(absolute.toPath()).toString()
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
