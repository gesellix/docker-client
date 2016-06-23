package de.gesellix.docker.client.util

class NamedPipeExplorationTest {

    public static void main(String[] args) {
        try {
            // Connect to the named pipe
            RandomAccessFile pipe = new RandomAccessFile("\\\\.\\pipe\\docker_engine", "rw")

            String req = "GET /_ping HTTP/1.1\r\nHost: localhost\r\n\r\n"
            // Write request to the pipe
            pipe.write(req.getBytes())

            // Read response from pipe
            String line
            while ((line = pipe.readLine()) != null) {
                println line
            }

            // Close the pipe
            pipe.close()

            // do something with res
        }
        catch (Exception e) {
            // do something
            println e
        }
    }
}
