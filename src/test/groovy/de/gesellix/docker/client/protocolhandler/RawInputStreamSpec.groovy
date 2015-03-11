package de.gesellix.docker.client.protocolhandler

import org.apache.commons.io.IOUtils
import spock.lang.Specification

import static de.gesellix.docker.client.protocolhandler.StreamType.STDERR
import static de.gesellix.docker.client.protocolhandler.StreamType.STDOUT

class RawInputStreamSpec extends Specification {

  def "should stream the complete payload"() {
    given:
    def actualText = "docker\nrocks!\n\n"
    def headerAndPayload = new RawHeaderAndPayload(STDOUT, actualText.bytes)
    def inputStream = new ByteArrayInputStream((byte[]) headerAndPayload.bytes)
    def outputStream = new ByteArrayOutputStream()

    when:
    def copied = IOUtils.copy(new RawInputStream(inputStream), outputStream)

    then:
    copied == actualText.size()
    and:
    outputStream.toString() == actualText
  }

  def "should stream the complete payload with empty final frame"() {
    given:
    def actualText = "docker\nstill\nrocks!"
    def headerAndPayload1 = new RawHeaderAndPayload(STDOUT, actualText.bytes)
    def headerAndPayload2 = new RawHeaderAndPayload(STDOUT, new byte[0])
    def bytes = headerAndPayload1.bytes + headerAndPayload2.bytes
    def inputStream = new ByteArrayInputStream((byte[]) bytes)
    def outputStream = new ByteArrayOutputStream()

    when:
    def copied = IOUtils.copy(new RawInputStream(inputStream), outputStream)

    then:
    copied == actualText.size()
    and:
    outputStream.toString() == actualText
  }

  def "should multiplex the stream"() {
    given:
    def actualText = "docker still rocks!"
    def actualError = "docker rocks again!"
    def headerAndPayload1 = new RawHeaderAndPayload(STDOUT, actualText.bytes)
    def headerAndPayload2 = new RawHeaderAndPayload(STDERR, actualError.bytes)
    def headerAndPayload3 = new RawHeaderAndPayload(STDOUT, new byte[0])
    def bytes = headerAndPayload1.bytes + headerAndPayload2.bytes + headerAndPayload3.bytes
    def inputStream = new ByteArrayInputStream((byte[]) bytes)
    def stdout = new ByteArrayOutputStream()
    def stderr = new ByteArrayOutputStream()

    when:
    def rawInputStream = new RawInputStream(inputStream)
    def copied = rawInputStream.copyFullyMultiplexed(stdout, stderr)

    then:
    copied == actualText.length() + actualError.length()
    and:
    stdout.toString() == actualText
    and:
    stderr.toString() == actualError
  }
}
