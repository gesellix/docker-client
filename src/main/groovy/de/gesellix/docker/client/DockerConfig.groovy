package de.gesellix.docker.client

class DockerConfig {

    def dockerHost = getDockerHostOrDefault()

    static def getDockerHostOrDefault(){
        def configuredDockerHost = System.getProperty("docker.host", System.env.DOCKER_HOST as String)
        if (configuredDockerHost) {
            return configuredDockerHost
        } else {
            if (System.properties['os.name'].toLowerCase().contains('windows')) {
                // default to non-tls http
                //return "tcp://localhost:2375"

                // or use a named pipe:
                return "npipe:////./pipe/docker_engine"
            } else {
                return "unix:///var/run/docker.sock"
            }
        }
    }

    def defaultTlsPort = 2376

    def tlsVerify = System.getProperty("docker.tls.verify", System.env.DOCKER_TLS_VERIFY as String)

    def certPath = System.getProperty("docker.cert.path", System.env.DOCKER_CERT_PATH as String)

    def defaultCertPath = new File(System.properties['user.home'] as String, ".docker")

    // the v1 registry still seems to be valid for authentication.
    def indexUrl_v1 = 'https://index.docker.io/v1/'
    def indexUrl_v2 = 'https://registry-1.docker.io'

    def configFile = new File("${System.getProperty('user.home')}/.docker", "config.json")

    def legacyConfigFile = new File("${System.getProperty('user.home')}", ".dockercfg")

    def dockerConfigFile = null

    def setDockerConfigFile(File dockerConfigFile) {
        this.dockerConfigFile = dockerConfigFile
    }

    File getDockerConfigFile() {
        if (dockerConfigFile == null) {
            String dockerConfig = System.getProperty("docker.config", System.env.DOCKER_CONFIG as String)
            if (dockerConfig) {
                this.dockerConfigFile = new File(dockerConfig, 'config.json')
            } else if (configFile.exists()) {
                this.dockerConfigFile = configFile
            } else if (legacyConfigFile.exists()) {
                this.dockerConfigFile = legacyConfigFile
            }
        }
        return dockerConfigFile
    }
}
