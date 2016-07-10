package de.gesellix.docker.client.util

class NullOutputStream extends OutputStream {

    @Override
    void write(int b) throws IOException {
        // -> /dev/null
    }
}
