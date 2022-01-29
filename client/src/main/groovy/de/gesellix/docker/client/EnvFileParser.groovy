package de.gesellix.docker.client

class EnvFileParser {

  List<String> parse(File file) {
    List<String> env = []
    file.eachLine { String line ->
      if (line.trim() && !line.matches("\\s*#.*")) {
        if (line.contains('=')) {
          env << line.replaceAll("^\\s*", "")
        }
        else {
          env << ("${line.trim()}=${System.getenv(line.trim())}" as String)
        }
      }
      return null
    }
    return env
  }
}
