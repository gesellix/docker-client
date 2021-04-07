package de.gesellix.docker.client.container

import de.gesellix.util.IOUtils
import groovy.util.logging.Slf4j
import okio.Okio
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream

@Slf4j
class ArchiveUtil {

  byte[] extractSingleTarEntry(InputStream tarContent, String filename) {
    def stream = new TarArchiveInputStream(new BufferedInputStream(tarContent))

    TarArchiveEntry entry = stream.nextTarEntry
    log.debug("entry size: ${entry.size}")

    def entryName = entry.name
    if (!filename.endsWith(entryName)) {
      log.warn("entry name '${entryName}' doesn't match expected filename '${filename}'")
    }
    else {
      log.debug("entry name: ${entryName}")
    }

    byte[] content = new byte[(int) entry.size]
    log.debug("going to read ${content.length} bytes")

    stream.read(content, 0, content.length)
    IOUtils.closeQuietly(stream)

    return content
  }

  int copySingleTarEntry(InputStream tarContent, String filename, OutputStream target) {
    def stream = new TarArchiveInputStream(new BufferedInputStream(tarContent))

    TarArchiveEntry entry = stream.nextTarEntry
    log.debug("entry size: ${entry.size}")

    def entryName = entry.name
    if (!filename.endsWith(entryName)) {
      log.warn("entry name '${entryName}' doesn't match expected filename '${filename}'")
    }
    else {
      log.debug("entry name: ${entryName}")
    }

    byte[] content = new byte[(int) entry.size]
    log.debug("going to read ${content.length} bytes")

    def source = Okio.source(stream)
    def sink = Okio.sink(target)
    IOUtils.copy(source, sink)
    sink.flush()
    sink.close()
    return entry.size
  }
}
