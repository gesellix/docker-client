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
    List<ILoggingEvent> loggedEvents = []

    static void clearLoggedEvents() {
        getMemoryAppender().clear()
    }

    static findLoggedEvent(needle) {
        MemoryAppender memoryAppender = getMemoryAppender()
        def events = memoryAppender.getLoggedEvents()
        return events.find { ILoggingEvent e -> e.level == needle.level && e.message == needle.message }
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
    void start() {
        try {
            encoder.start()
            super.start()
        }
        catch (IOException e) {
            started = false
            def errorStatus = new ErrorStatus("Failed to initialize encoder for appender named [" + name + "].", this, e)
            println errorStatus
            addStatus(errorStatus)
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return
        }

        try {
            event.prepareForDeferredProcessing()
            encoder.encode(event)
            loggedEvents.add(event)
        }
        catch (IOException ioe) {
            started = false
            addStatus(new ErrorStatus("IO failure in appender", this, ioe))
        }
    }

    List<ILoggingEvent> getLoggedEvents() {
        return new ArrayList<ILoggingEvent>(loggedEvents)
    }

    void clear() {
        loggedEvents.clear()
    }
}
