package de.gesellix.docker.compose

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import de.gesellix.docker.compose.adapters.ListToPortConfigsAdapter
import de.gesellix.docker.compose.adapters.MapOrListToEnvironmentAdapter
import de.gesellix.docker.compose.adapters.MapOrListToLabelAdapter
import de.gesellix.docker.compose.adapters.MapToDriverOptsAdapter
import de.gesellix.docker.compose.adapters.MapToExternalAdapter
import de.gesellix.docker.compose.adapters.StringOrListToCommandAdapter
import de.gesellix.docker.compose.adapters.StringToNetworkAdapter
import de.gesellix.docker.compose.interpolation.ComposeInterpolator
import de.gesellix.docker.compose.types.ComposeConfig
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import org.yaml.snakeyaml.Yaml

@Slf4j
class ComposeFileReader {

    // UnsupportedProperties not yet supported by this implementation of the compose file
    def UnsupportedProperties = [
            "build",
            "cap_add",
            "cap_drop",
            "cgroup_parent",
            "devices",
            "dns",
            "dns_search",
            "domainname",
            "external_links",
            "ipc",
            "links",
            "mac_address",
            "network_mode",
            "privileged",
            "read_only",
            "restart",
            "security_opt",
            "shm_size",
            "stop_signal",
            "sysctls",
            "tmpfs",
            "userns_mode",
    ]

    // DeprecatedProperties that were removed from the v3 format, but their use should not impact the behaviour of the application.
    def DeprecatedProperties = [
            "container_name": "Setting the container name is not supported.",
            "expose"        : "Exposing ports is unnecessary - services on the same network can access each other's containers on any port.",
    ]

    // ForbiddenProperties that are not supported in this implementation of the compose file.
    def ForbiddenProperties = [
            "extends"      : "Support for `extends` is not implemented yet.",
            "volume_driver": "Instead of setting the volume driver on the service, define a volume using the top-level `volumes` option and specify the driver there.",
            "volumes_from" : "To share a volume between services, define it using the top-level `volumes` option and reference it from each service that shares it using the service-level `volumes` option.",
            "cpu_quota"    : "Set resource limits using deploy.resources",
            "cpu_shares"   : "Set resource limits using deploy.resources",
            "cpuset"       : "Set resource limits using deploy.resources",
            "mem_limit"    : "Set resource limits using deploy.resources",
            "memswap_limit": "Set resource limits using deploy.resources",
    ]

    ComposeInterpolator interpolator = new ComposeInterpolator()

    Map<String, Object> loadYaml(InputStream composeFile) {
        Map<String, Object> composeContent = new Yaml().load(composeFile)
        log.info("composeContent: $composeContent}")

        return composeContent
    }

    ComposeConfig load(InputStream composeFile) {
        Map<String, Object> composeContent = loadYaml(composeFile)
        interpolator.interpolate(composeContent)

        Moshi moshi = new Moshi.Builder()
                .add(new ListToPortConfigsAdapter())
                .add(new MapOrListToEnvironmentAdapter())
                .add(new MapOrListToLabelAdapter())
                .add(new MapToDriverOptsAdapter())
                .add(new MapToExternalAdapter())
                .add(new StringOrListToCommandAdapter())
                .add(new StringToNetworkAdapter())
                .build()
        JsonAdapter<ComposeConfig> jsonAdapter = moshi.adapter(ComposeConfig)

        def json = JsonOutput.toJson(composeContent)
        ComposeConfig cfg = jsonAdapter.fromJson(json)

//        def forbiddenProperties = collectForbiddenServiceProperties(composeContent.services, ForbiddenProperties)
//        if (forbiddenProperties) {
//            log.error("Configuration contains forbidden properties: ${forbiddenProperties}")
//            throw new UnsupportedOperationException("Configuration contains forbidden properties")
//        }
//        def valid = new SchemaValidator().validate(composeContent)

//        composeContent.services.each{ serviceName, serviceDef ->
//        }

//        if services, ok := configDict["services"]; ok {
//            servicesConfig, err := interpolation.Interpolate(services.(types.Dict), "service", os.LookupEnv)
//            if err != nil {
//                return nil, err
//            }
//
//            servicesList, err := loadServices(servicesConfig, configDetails.WorkingDir)
//            if err != nil {
//                return nil, err
//            }
//
//            cfg.services = servicesList
//        }

//        if networks, ok := configDict["networks"]; ok {
//            networksConfig, err := interpolation.Interpolate(networks.(types.Dict), "network", os.LookupEnv)
//            if err != nil {
//                return nil, err
//            }
//
//            networksMapping, err := loadNetworks(networksConfig)
//            if err != nil {
//                return nil, err
//            }
//
//            cfg.NetworksType = networksMapping
//        }

//        if volumes, ok := configDict["volumes"]; ok {
//            volumesConfig, err := interpolation.Interpolate(volumes.(types.Dict), "volume", os.LookupEnv)
//            if err != nil {
//                return nil, err
//            }
//
//            volumesMapping, err := loadVolumes(volumesConfig)
//            if err != nil {
//                return nil, err
//            }
//
//            cfg.Volumes = volumesMapping
//        }

//        if secrets, ok := configDict["secrets"]; ok {
//            secretsConfig, err := interpolation.Interpolate(secrets.(types.Dict), "secret", os.LookupEnv)
//            if err != nil {
//                return nil, err
//            }
//
//            secretsMapping, err := loadSecrets(secretsConfig, configDetails.WorkingDir)
//            if err != nil {
//                return nil, err
//            }
//
//            cfg.Secrets = secretsMapping
//        }

        return cfg
    }

    def collectForbiddenServiceProperties(services, Map<String, String> forbiddenProperties) {
        def hits = [:]
        services.each { service, serviceConfig ->
            forbiddenProperties.each { property, description ->
                if (serviceConfig[property]) {
                    hits["$service.$property"] = description
                }
            }
        }
        return hits
    }

    def collectUnsupportedServiceProperties(services, List<String> unsupportedProperties) {
        def hits = [:]
        services.each { service, serviceConfig ->
            unsupportedProperties.each { property ->
                if (serviceConfig[property]) {
                    hits["$service.$property"]
                }
            }
        }
        return hits
    }
}
