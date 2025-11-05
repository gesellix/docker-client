package de.gesellix.docker.client.container;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

public class ArchiveUtil {

  private final Logger log = LoggerFactory.getLogger(ArchiveUtil.class);

  /**
   * @see #copySingleTarEntry(InputStream, String, OutputStream)
   * @deprecated use #copySingleTarEntry(java.io.InputStream, java.lang.String, java.io.OutputStream)
   */
  @Deprecated
  public byte[] extractSingleTarEntry(InputStream tarContent, String filename) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    long bytesRead = copySingleTarEntry(tarContent, filename, output);
    return output.toByteArray();
  }

  /**
   * Closes the tarContent InputStream and the target OutputStream when done.
   */
  public long copySingleTarEntry(InputStream tarContent, String filename, OutputStream target) {
    TarArchiveInputStream stream = new TarArchiveInputStream(new BufferedInputStream(tarContent));

    final TarArchiveEntry entry;
    try {
      entry = stream.getNextEntry();
      log.debug("entry size: {}", entry.getSize());
    } catch (Exception e) {
      throw new RuntimeException("failed to get next TarArchiveEntry", e);
    }

    final String entryName = entry.getName();
    if (!filename.endsWith(entryName)) {
      log.warn("entry name '{}' doesn't match expected filename '{}'", entryName, filename);
    } else {
      log.debug("entry name: '{}'", entryName);
    }

    final byte[] content = new byte[(int) entry.getSize()];
    log.debug("going to read {} bytes", content.length);

    try {
      try (BufferedSink sink = Okio.buffer(Okio.sink(target));
           BufferedSource source = Okio.buffer(Okio.source(stream))) {
        long read = source.readAll(sink);
        sink.flush();
//        return read;
      }
      return entry.getSize();
    } catch (Exception e) {
      throw new RuntimeException("failed to write TarArchiveEntry to target OutputStream", e);
    }
  }

  private void silently(Callable<Void> action) {
    try {
      action.call();
    } catch (Exception ignored) {
    }
  }
}
