package de.gesellix.docker.client

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.io.IOUtils

// with modifications based on https://github.com/docker-java/docker-java/blob/master/src/main/java/com/github/dockerjava/client/utils/CompressArchiveUtil.java
class TarFileBuilder {

  public static File archiveTarFiles(File base, Iterable<File> files, String archiveNameWithOutExtension) throws IOException {
    File tarFile = File.createTempFile(archiveNameWithOutExtension, ".tar")
    tarFile.deleteOnExit()
    TarArchiveOutputStream tos = new TarArchiveOutputStream(new FileOutputStream(tarFile))
    try {
      tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
      for (File file : files) {
        TarArchiveEntry tarEntry = new TarArchiveEntry(file)
        tarEntry.setName(relativize(base, file))

        tos.putArchiveEntry(tarEntry)

        if (!file.isDirectory()) {
          copyFile(file, tos)
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

  private static long copyFile(File input, OutputStream output) throws IOException {
    final FileInputStream fis = new FileInputStream(input);
    try {
      return IOUtils.copyLarge(fis, output);
    }
    finally {
      fis.close();
    }
  }
}
