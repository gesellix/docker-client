package de.gesellix.docker.testutil

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.OutputStreamAppender
import org.slf4j.LoggerFactory

import static org.slf4j.Logger.ROOT_LOGGER_NAME

/**
 * From https://github.com/grails-plugins/grails-logback/blob/master/src/java/grails/plugin/logback/MemoryAppender.java
 *
 * @author <a href='mailto:burt@burtbeckwith.com'>Burt Beckwith</a>
 */
class MemoryAppender extends OutputStreamAppender<ILoggingEvent> {

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
        setOutputStream(new ByteArrayOutputStream())
        super.start()
    }

    @Override
    protected void subAppend(ILoggingEvent event) {
        super.subAppend(event)
        loggedEvents.add(event)
    }

    List<ILoggingEvent> getLoggedEvents() {
        return new ArrayList<ILoggingEvent>(loggedEvents)
    }

    void clear() {
        loggedEvents.clear()
    }
}
