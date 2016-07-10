package de.gesellix.docker.testutil

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.UnsynchronizedAppenderBase
import ch.qos.logback.core.encoder.Encoder
import ch.qos.logback.core.status.ErrorStatus
import org.slf4j.LoggerFactory

import static org.slf4j.Logger.ROOT_LOGGER_NAME

/**
 * From https://github.com/grails-plugins/grails-logback/blob/master/src/java/grails/plugin/logback/MemoryAppender.java
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class MemoryAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    Encoder<ILoggingEvent> encoder
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    List<ILoggingEvent> loggedEvents = []

    static void clearLoggedEvents() {
        getMemoryAppender().clear()
    }

    static def findLoggedEvent(needle) {
        MemoryAppender memoryAppender = getMemoryAppender()
        def events = memoryAppender.getLoggedEvents()
        events.find { ILoggingEvent e -> e.level == needle.level && e.message == needle.message }
        return events
    }

    static MemoryAppender getMemoryAppender() {
        Logger rootLogger = LoggerFactory.getLogger(ROOT_LOGGER_NAME) as Logger
        def memoryAppender = rootLogger.iteratorForAppenders().find { it instanceof MemoryAppender }
        if (!memoryAppender) {
            throw new IllegalStateException("Didn't find MemoryAppender. Please check your logback(-test) config.")
        }
        return memoryAppender as MemoryAppender
    }

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

    public void clear() {
        loggedEvents.clear()
    }

    public String getRenderedOutput() {
        return new String(outputStream.toByteArray())
    }
}
