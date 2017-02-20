package de.gesellix.docker.compose.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.ToJson
import de.gesellix.docker.compose.types.PortConfig
import de.gesellix.docker.compose.types.PortConfigs

class ListToPortConfigsAdapter {

    @ToJson
    List<Map<String, Object>> toJson(@PortConfigsType PortConfigs portConfigs) {
        throw new UnsupportedOperationException()
    }

    @FromJson
    @PortConfigsType
    PortConfigs fromJson(JsonReader reader) {
        def portConfigs = []
        def token = reader.peek()
        if (token == JsonReader.Token.BEGIN_ARRAY) {
            reader.beginArray()
            while (reader.hasNext()) {
                portConfigs.addAll(parsePortConfigEntry(reader))
            }
            reader.endArray()
        } else {
            // ...
        }
        return new PortConfigs(portConfigs: portConfigs)
    }

    List<PortConfig> parsePortConfigEntry(JsonReader reader) {
        def entryToken = reader.peek()
        if (entryToken == JsonReader.Token.NUMBER) {
            def value = Integer.toString(reader.nextInt())
            return parsePortDefinition(value)
        } else if (entryToken == JsonReader.Token.STRING) {
            def value = reader.nextString()
            return parsePortDefinition(value)
        } else if (entryToken == JsonReader.Token.BEGIN_OBJECT) {
            reader.beginObject()
            def portConfig = new PortConfig()
            while (reader.hasNext()) {
                def name = reader.nextName()
                def valueType = reader.peek()
                if (valueType == JsonReader.Token.STRING) {
                    def value = reader.nextString()
                    portConfig[name] = value
                } else if (valueType == JsonReader.Token.NUMBER) {
                    def value = reader.nextInt()
                    portConfig[name] = value
                } else {
                    // ...
                }
            }
            reader.endObject()
            return [portConfig]
        } else {
            // ...
        }
        return []
    }

    List<PortConfig> parsePortDefinition(String portSpec) {
        def (rawIP, hostPort, containerPort) = splitParts(portSpec)
        def (proto, plainContainerPort) = splitProto(containerPort as String)

        if (!plainContainerPort) {
            throw new IllegalStateException("No port specified: '${portSpec}'")
        }

        validateProto(proto as String)

        if (rawIP) {
            def address = InetAddress.getByName("$rawIP")
            rawIP = address.hostAddress
        }

        def (startPort, endPort) = parsePortRange(plainContainerPort as String)

        def (startHostPort, endHostPort) = [0, 0]
        if ((hostPort as String).length() > 0) {
            (startHostPort, endHostPort) = parsePortRange(hostPort as String)

            if ((endPort - startPort) != (endHostPort - startHostPort)) {
                // Allow host port range if containerPort is not a range.
                // In this case, use the host port range as the dynamic
                // host port range to allocate into.
                if (endPort != startPort) {
                    throw new IllegalStateException("Invalid ranges specified for container and host Ports: '$containerPort' and '$hostPort'")
                }
            }
        }

        def portMappings = []
        for (int i = 0; i <= (endPort - startPort); i++) {
            containerPort = "${startPort + i}"
            if ((hostPort as String).length() > 0) {
                hostPort = "${startHostPort + i}"
            }
            // Set hostPort to a range only if there is a single container port
            // and a dynamic host port.
            if (startPort == endPort && startHostPort != endHostPort) {
                hostPort = "${hostPort}-${endHostPort}"
            }
            def port = newPort((proto as String).toLowerCase(), containerPort)
            portMappings.add([
                    port   : port,
                    binding: [
                            proto   : (proto as String).toLowerCase(),
                            hostIP  : rawIP,
                            hostPort: hostPort]])
        }

        Set<String> exposedPorts = new TreeSet<String>()
        Map<String, List> bindings = [:]

        portMappings.each { portMapping ->
            String port = portMapping.port
            exposedPorts.add(port)

            if (!bindings.containsKey(port)) {
                bindings[port] = []
            }
            bindings[port].add(portMapping.binding)
        }

        def portConfigs = []
        exposedPorts.each { port ->
            bindings[port].each { binding ->
                int hostPortAsInt = 0
                if (binding.hostPort != "") {
                    hostPortAsInt = binding.hostPort as int
                }
                portConfigs.add(new PortConfig(
                        protocol: binding.proto,
                        target: port.split('/')[0] as int,
                        published: hostPortAsInt,
                        mode: "ingress"
                ))
            }
        }
        return portConfigs
    }

    // newPort creates a new instance of a port String given a protocol and port number or port range
    String newPort(String proto, String port) {
        // Check for parsing issues on "port" now so we can avoid having
        // to check it later on.

        def (portStartInt, portEndInt) = [0, 0]
        if (port.length() > 0) {
            (portStartInt, portEndInt) = parsePortRange(port)
        }

        if (portStartInt == portEndInt) {
            return "${portStartInt}/${proto}"
        }
        return "${portStartInt}-${portEndInt}/${proto}"
    }

    def parsePortRange(String ports) {
        if (!ports.contains('-')) {
            return [ports as Integer, ports as Integer]
        }

        def startAndEnd = ports.split('-')
        def start = startAndEnd[0] as Integer
        def end = startAndEnd[1] as Integer
        if (end < start) {
            throw new IllegalStateException("Invalid range specified for the Port '$ports'")
        }
        return [start, end]
    }

    def validateProto(String proto) {
        if (!["tcp", "udp"].contains(proto.toLowerCase())) {
            throw new IllegalStateException("Invalid proto '$proto'")
        }
    }

    def splitProto(String rawPort) {
        def parts = rawPort.split('/')
        if (rawPort.length() == 0 || parts.length == 0 || parts[0].length() == 0) {
            return ["", ""]
        }

        if (parts.length == 1) {
            return ["tcp", rawPort]
        }
        if (parts[1].length() == 0) {
            return ["tcp", parts[0]]
        }
        return [parts[1], parts[0]]
    }

    def splitParts(String rawPort) {
        def parts = rawPort.split(':')
        switch (parts.length) {
            case 1:
                return ["", "", parts[0]]
            case 2:
                return ["", parts[0], parts[1]]
            case 3:
                return [parts[0], parts[1], parts[2]]
            default:
                return [parts.take(parts.length - 2).join(':'), parts[parts.length - 2], parts.length - 1]
        }
    }
}
