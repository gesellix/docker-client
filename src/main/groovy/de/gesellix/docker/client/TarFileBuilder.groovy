package de.gesellix.docker.client

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.io.FileUtils

// from https://github.com/docker-java/docker-java/blob/master/src/main/java/com/github/dockerjava/client/utils/CompressArchiveUtil.java
class TarFileBuilder {

  public static File archiveTarFiles(File base, Iterable<File> files, String archiveNameWithOutExtension) throws IOException {
    File tarFile = new File(FileUtils.getTempDirectoryPath(), archiveNameWithOutExtension + ".tar")
    tarFile.deleteOnExit()
    TarArchiveOutputStream tos = new TarArchiveOutputStream(new FileOutputStream(tarFile))
    try {
      tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
      for (File file : files) {
        TarArchiveEntry tarEntry = new TarArchiveEntry(file)
        tarEntry.setName(relativize(base, file))

        tos.putArchiveEntry(tarEntry)

        if (!file.isDirectory()) {
          FileUtils.copyFile(file, tos)
        }
        tos.closeArchiveEntry()
      }
    }
    finally {
      tos.close()
    }

    return tarFile
  }

  private static String relativize(File base, File absolute) {
    String relative = base.toURI().relativize(absolute.toURI()).getPath()
    return relative
  }
}
