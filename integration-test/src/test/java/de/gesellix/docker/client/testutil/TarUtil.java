package de.gesellix.docker.client.testutil;

import okio.BufferedSink;
import okio.Okio;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class TarUtil {

  public File unTar(File tarFile) throws IOException {
    return unTar(new FileInputStream(tarFile));
  }

  public File unTar(InputStream tarFile) throws IOException {
    File destDir = Files.createTempDirectory("de-gesellix-tests").toFile();
    destDir.deleteOnExit();

    TarArchiveInputStream tis = new TarArchiveInputStream(tarFile);
    TarArchiveEntry tarEntry;
    while ((tarEntry = tis.getNextTarEntry()) != null) {
      File outputFile = new File(destDir, tarEntry.getName());
      if (tarEntry.isDirectory()) {
        if (!outputFile.exists()) {
          outputFile.mkdirs();
        }
      }
      else {
        outputFile.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(outputFile);
        BufferedSink sink = Okio.buffer(Okio.sink(fos));
        sink.writeAll(Okio.buffer(Okio.source(tis)));
        sink.flush();
        sink.close();
        fos.close();
      }
    }
    tis.close();
    return destDir;
  }
}
