package de.gesellix.docker.engine.client.infrastructure;

import java.nio.charset.StandardCharsets;

public class Frame {

  private final StreamType streamType;
  private final byte[] payload;

  public Frame(StreamType streamType, byte[] payload) {
    this.streamType = streamType;
    this.payload = payload;
  }

  public StreamType getStreamType() {
    return streamType;
  }

  public byte[] getPayload() {
    return payload;
  }

  public String getPayloadAsString() {
    return new String(payload, StandardCharsets.UTF_8).trim();
  }

  @Override
  public String toString() {
    return "Frame{" +
           "streamType=" + streamType +
           ", payload=" + getPayloadAsString() +
           '}';
  }

  /**
   * STREAM_TYPE can be:
   * <ul>
   * <li>0: stdin (will be written on stdout)</li>
   * <li>1: stdout</li>
   * <li>2: stderr</li>
   * <li>3: systemerr</li>
   * </ul>
   * See the paragraph _Stream format_ at https://docs.docker.com/engine/api/v1.41/#operation/ContainerAttach.
   * Reference implementation: https://github.com/moby/moby/blob/master/pkg/stdcopy/stdcopy.go.
   * Docker client GoDoc: https://godoc.org/github.com/moby/moby/client#Client.ContainerAttach.
   */
  public enum StreamType {

    STDIN((byte) 0),
    STDOUT((byte) 1),
    STDERR((byte) 2),
    SYSTEMERR((byte) 3);

    StreamType(Object streamTypeId) {
      this.streamTypeId = ((byte) (streamTypeId));
    }

    public static StreamType valueOf(final byte b) {
      switch (b) {
        case 0:
          return STDIN;
        case 1:
          return STDOUT;
        case 2:
          return STDERR;
        case 3:
          return SYSTEMERR;
        default:
          throw new IllegalArgumentException("no enum value for " + String.valueOf(b) + " found.");
      }
    }

    public byte getStreamTypeId() {
      return streamTypeId;
    }

    private final byte streamTypeId;
  }
}
