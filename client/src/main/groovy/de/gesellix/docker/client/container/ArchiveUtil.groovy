package de.gesellix.docker.client.container

import de.gesellix.util.IOUtils
import okio.Okio
import okio.Sink
import okio.Source
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.Callable

class ArchiveUtil {

  private final Logger log = LoggerFactory.getLogger(ArchiveUtil)

  /**
   * @deprecated use #copySingleTarEntry(java.io.InputStream, java.lang.String, java.io.OutputStream)
   * @see #copySingleTarEntry(java.io.InputStream, java.lang.String, java.io.OutputStream)
   */
  @Deprecated
  byte[] extractSingleTarEntry(InputStream tarContent, String filename) {
    ByteArrayOutputStream output = new ByteArrayOutputStream()
    def bytesRead = copySingleTarEntry(tarContent, filename, output)
    return output.toByteArray()
  }

  /**
   * Closes the tarContent InputStream and the target OutputStream when done.
   */
  int copySingleTarEntry(InputStream tarContent, String filename, OutputStream target) {
    TarArchiveInputStream stream = new TarArchiveInputStream(new BufferedInputStream(tarContent))

    TarArchiveEntry entry = stream.nextTarEntry
    log.debug("entry size: ${entry.size}")

    String entryName = entry.name
    if (!filename.endsWith(entryName)) {
      log.warn("entry name '${entryName}' doesn't match expected filename '${filename}'")
    }
    else {
      log.debug("entry name: ${entryName}")
    }

    byte[] content = new byte[(int) entry.size]
    log.debug("going to read ${content.length} bytes")

    Source source = Okio.source(stream)
    Sink sink = Okio.sink(target)
    try {
      IOUtils.copy(source, sink)
      return entry.size
    }
    finally {
      if (sink != null) {
        silently { sink.flush() }
        silently { sink.close() }
      }
      if (source != null) {
        silently { source.close() }
      }
    }
  }

  private void silently(Callable<Void> action) {
    try {
      action()
    }
    catch (Exception ignored) {}
  }
}
