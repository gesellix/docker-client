package de.gesellix.docker.client.builder

import de.gesellix.docker.client.util.IOUtils
import groovy.util.logging.Slf4j
import okio.Okio
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream

import java.nio.file.Files
import java.util.zip.GZIPOutputStream

@Slf4j
class BuildContextBuilder {

    static archiveTarFilesRecursively(File base, File targetFile) throws IOException {
        def filenames = new DockerignoreFileFilter(base, [targetFile.absolutePath]).collectFiles(base)
        log.debug "found ${filenames.size()} files in buildContext."
        archiveTarFiles(base, filenames, targetFile)
    }

    static archiveTarFiles(File base, filenames, File targetFile) throws IOException {
        TarArchiveOutputStream tos = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(targetFile)))
        try {
            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
            for (String filename : filenames) {
                def relativeFileName = relativize(base, new File(filename))
                log.debug "adding ${filename} as ${relativeFileName}"
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

    static String relativize(File base, File absolute) {
        return base.toPath().relativize(absolute.toPath()).toString()
    }

    static copyFile(File input, OutputStream output) throws IOException {
        def source = null
        try {
            source = Okio.source(input)
            IOUtils.copy(source, Okio.sink(output))
        }
        finally {
            IOUtils.closeQuietly(source)
        }
    }
}
