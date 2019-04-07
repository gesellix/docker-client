package de.gesellix.docker.client

class EnvFileParser {

    List<String> parse(File file) {
        List<String> env = []
        file.eachLine { line ->
            if (line.trim() && !line.matches("\\s*#.*")) {
                if (line.contains('=')) {
                    env << line.replaceAll("^\\s*", "")
                } else {
                    env << ("${line.trim()}=${System.env[line.trim()]}" as String)
                }
            }
            return null
        }
        return env
    }
}
