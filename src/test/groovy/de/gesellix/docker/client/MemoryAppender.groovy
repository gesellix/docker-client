package de.gesellix.docker.client

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.status.ErrorStatus

/**
 * From https://github.com/grails-plugins/grails-logback/blob/master/src/java/grails/plugin/logback/MemoryAppender.java
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class MemoryAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    Encoder<ILoggingEvent> encoder
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    List<ILoggingEvent> loggedEvents = []

    @Override
    public void start() {
        try {
            encoder.init(outputStream)
            super.start()
        }
        catch (IOException e) {
            started = false
            addStatus(new ErrorStatus("Failed to initialize encoder for appender named [" + name + "].", this, e))
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return
        }

        try {
            event.prepareForDeferredProcessing()
            encoder.doEncode(event)
            loggedEvents.add(event)
        }
        catch (IOException ioe) {
            started = false
            addStatus(new ErrorStatus("IO failure in appender", this, ioe))
        }
    }

    public List<ILoggingEvent> getLoggedEvents() {
        return new ArrayList<ILoggingEvent>(loggedEvents)
    }

    public void clearLoggedEvents() {
        loggedEvents.clear()
    }

    public String getRenderedOutput() {
        return new String(outputStream.toByteArray())
    }
}
