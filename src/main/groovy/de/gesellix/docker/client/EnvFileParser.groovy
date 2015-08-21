package de.gesellix.docker.client

class EnvFileParser {

    def parse(File file) {
        def env = []
        file.eachLine { line ->
            if (line.trim() && !line.matches("\\s*#.*")) {
                if (line.contains('=')) {
                    env << line.replaceAll("^\\s*", "")
                } else {
                    env << "${line.trim()}=${System.env[line.trim()]}"
                }
            }
        }
        return env
    }
}
