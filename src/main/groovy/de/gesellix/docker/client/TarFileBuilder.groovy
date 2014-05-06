package de.gesellix.docker.client

import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.io.IOUtils

class TarFileBuilder {

  def doIt() {
    /* Output Stream - that will hold the physical TAR file */
    OutputStream tar_output = new FileOutputStream(new File("tar_ball.tar"))

    /* Create Archive Output Stream that attaches File Output Stream / and specifies TAR as type of compression */
    ArchiveOutputStream my_tar_ball = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.TAR, tar_output)

    /* Create Archieve entry - write header information*/
    File tar_input_file = new File("test_file_1.xml")
    TarArchiveEntry tar_file = new TarArchiveEntry(tar_input_file)

    /* length of the TAR file needs to be set using setSize method */
    tar_file.setSize(tar_input_file.length())
    my_tar_ball.putArchiveEntry(tar_file)
    IOUtils.copy(new FileInputStream(tar_input_file), my_tar_ball)

    /* Close Archieve entry, write trailer information */
    my_tar_ball.closeArchiveEntry()

    /* Repeat steps for the next file that needs to be added to the TAR */
    tar_input_file = new File("test_file_2.xml")
    tar_file = new TarArchiveEntry(tar_input_file)
    tar_file.setSize(tar_input_file.length())
    my_tar_ball.putArchiveEntry(tar_file)
    IOUtils.copy(new FileInputStream(tar_input_file), my_tar_ball)

    /* Close Archieve entry, write trailer information */
    my_tar_ball.closeArchiveEntry()
    my_tar_ball.finish()

    /* Close output stream, our files are zipped */
    tar_output.close()
  }
}
