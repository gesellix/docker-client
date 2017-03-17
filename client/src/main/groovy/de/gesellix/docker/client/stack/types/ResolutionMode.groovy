package de.gesellix.docker.client.stack.types

enum ResolutionMode {

    ResolutionModeVIP("vip"),
    ResolutionModeDNSRR("dnsrr")

    final String value

    ResolutionMode(String value) {
        this.value = value
    }

    static ResolutionMode byValue(String needle) {
        def entry = values().find { it.value == needle }
        if (!entry) {
            throw new IllegalArgumentException("no enum found for ${needle}")
        }
        return entry
    }
}
