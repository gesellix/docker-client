package de.gesellix.docker.client

class NullOutputStream extends OutputStream {

    @Override
    void write(int b) throws IOException {
        // -> /dev/null
    }
}
