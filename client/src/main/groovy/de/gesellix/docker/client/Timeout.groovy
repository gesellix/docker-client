package de.gesellix.docker.client

import java.util.concurrent.TimeUnit

import static java.util.concurrent.TimeUnit.MINUTES

class Timeout {
    long timeout
    TimeUnit unit

    final static Timeout TEN_MINUTES = new Timeout(10, MINUTES)

    Timeout(long timeout, TimeUnit unit) {
        this.timeout = timeout
        this.unit = unit
    }

    @Override
    String toString() {
        return "$timeout $unit"
    }
}
