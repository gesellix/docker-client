package de.gesellix.docker.client.protocolhandler

class RawHeaderAndPayload {
  private StreamType streamType
  private byte[] payload

  RawHeaderAndPayload(streamType, payload) {
    this.streamType = streamType
    this.payload = payload
  }

  def getBytes() {
    def bytes = [
        streamType.streamTypeId,
        0, 0, 0,
        // assume a short payload
        0, 0, 0, payload.length
    ]
    payload.each {
      bytes << it
    }
    return bytes
  }
}
