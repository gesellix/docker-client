package de.gesellix.docker.engine.client.infrastructure

import de.gesellix.docker.response.Reader
import okio.BufferedSource
import okio.Source
import okio.buffer

class FrameReader(source: Source) : Reader<Frame> {

  private val buffer: BufferedSource = source.buffer()

  override fun readNext(type: Class<Frame>?): Frame {
    // Stream format: https://docs.docker.com/engine/api/v1.41/#operation/ContainerAttach
    // header := [8]byte{STREAM_TYPE, 0, 0, 0, SIZE1, SIZE2, SIZE3, SIZE4}

    val streamType = Frame.StreamType.valueOf(buffer.readByte())
    buffer.skip(3)
    val frameSize = buffer.readInt()

    return Frame(streamType, buffer.readByteArray(frameSize.toLong()))
  }

  override fun hasNext(): Boolean {
    return !Thread.currentThread().isInterrupted && !buffer.exhausted()
  }
}
