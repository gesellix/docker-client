package de.gesellix.docker.client.protocolhandler

class RawDockerHeader {
  def streamType
  def frameSize

  RawDockerHeader(DataInputStream dataInput) {
    streamType = dataInput.readByte()
    dataInput.readByte()
    dataInput.readByte()
    dataInput.readByte()

    // http://stackoverflow.com/questions/13203426/convert-4-bytes-to-an-unsigned-32-bit-integer-and-storing-it-in-a-long
    frameSize = dataInput.readInt()
  }
}
