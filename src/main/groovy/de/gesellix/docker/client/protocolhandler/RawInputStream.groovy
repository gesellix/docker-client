package de.gesellix.docker.client.protocolhandler

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static de.gesellix.docker.client.protocolhandler.RawStreamHeader.EMPTY_HEADER

/**
 * see https://docs.docker.com/reference/api/docker_remote_api_v1.17/#attach-to-a-container.
 */
class RawInputStream extends FilterInputStream {

  Logger logger = LoggerFactory.getLogger(RawInputStream)

  def RawInputStream(InputStream inputStream) {
    super(inputStream)
  }

  def remainingFrameSize = -1

  @Override
  synchronized int read(byte[] b, int off, int len) throws IOException {
    if (remainingFrameSize <= 0) {
      def parsedHeader = readFrameHeader()
      logger.trace(parsedHeader.toString())
      if (parsedHeader == EMPTY_HEADER) {
        return -1
      }
      remainingFrameSize = parsedHeader.frameSize
    }

    def count = readRemainingFrameSize(b, off, len, remainingFrameSize)
    remainingFrameSize -= (count >= 0 ? count : 0)
    return count
  }

  def readFrameHeader() {
    int[] headerBuf = [
        read(), read(), read(), read(),
        read(), read(), read(), read()]

//    logger.trace("read header: ${headerBuf}")
    if (headerBuf.find { it < 0 }) {
      return EMPTY_HEADER
    }

    def parsedHeader = new RawStreamHeader(headerBuf)
    logger.trace(parsedHeader.toString())
    return parsedHeader
  }

  def readRemainingFrameSize(byte[] b, int off, int len, int remainingFrameSize) {
    def updatedLen = Math.min(len, remainingFrameSize)
    return super.read(b, off, updatedLen)
  }
}
