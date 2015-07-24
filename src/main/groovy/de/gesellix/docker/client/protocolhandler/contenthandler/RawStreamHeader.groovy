package de.gesellix.docker.client.protocolhandler.contenthandler

/**
 * see https://docs.docker.com/reference/api/docker_remote_api_v1.17/#attach-to-a-container.
 */
class RawStreamHeader {

  final static EMPTY_HEADER = new RawStreamHeader()

  def streamType
  def frameSize

  private RawStreamHeader() {
  }

  RawStreamHeader(int[] header) {
    if (header == null || header.length != 8) {
      throw new IllegalArgumentException("needs a header with a length of 8, got ${header?.length}.")
    }

    // header[0] is the streamType
    streamType = readDockerStreamType(header)
    // header[1-3] will be ignored, since they're currently not used
    // header[4-7] is the frameSize
    frameSize = readFrameSize(header)
  }

  StreamType readDockerStreamType(int[] header) throws IOException {
    if (header[0] < 0) {
      throw new EOFException("${header[0]}")
    }
    return StreamType.valueOf((byte) (header[0] & 0xFF))
  }

  int readFrameSize(int[] header) throws IOException {
    int ch1 = header[4]
    int ch2 = header[5]
    int ch3 = header[6]
    int ch4 = header[7]
    if ((ch1 | ch2 | ch3 | ch4) < 0) {
      throw new EOFException("$ch1 $ch2 $ch3 $ch4")
    }
    return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0)
  }

  @Override
  public String toString() {
    return "RawDockerHeader{streamType=${streamType}, frameSize=${frameSize}}"
  }
}
