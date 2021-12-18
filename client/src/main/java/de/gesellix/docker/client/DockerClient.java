package de.gesellix.docker.client;

import de.gesellix.docker.client.authentication.ManageAuthentication;
import de.gesellix.docker.client.checkpoint.ManageCheckpoint;
import de.gesellix.docker.client.config.ManageConfig;
import de.gesellix.docker.client.container.ManageContainer;
import de.gesellix.docker.client.distribution.ManageDistribution;
import de.gesellix.docker.client.image.ManageImage;
import de.gesellix.docker.client.network.ManageNetwork;
import de.gesellix.docker.client.node.ManageNode;
import de.gesellix.docker.client.plugin.ManagePlugin;
import de.gesellix.docker.client.secret.ManageSecret;
import de.gesellix.docker.client.service.ManageService;
import de.gesellix.docker.client.stack.ManageStack;
import de.gesellix.docker.client.swarm.ManageSwarm;
import de.gesellix.docker.client.system.ManageSystem;
import de.gesellix.docker.client.tasks.ManageTask;
import de.gesellix.docker.client.volume.ManageVolume;

public interface DockerClient
    extends ManageAuthentication,
            ManageCheckpoint,
            ManageContainer,
            ManageImage,
            ManageDistribution,
            ManageNetwork,
            ManageNode,
            ManagePlugin,
            ManageSecret,
            ManageConfig,
            ManageService,
            ManageStack,
            ManageSwarm,
            ManageSystem,
            ManageTask,
            ManageVolume {

  String getSwarmMangerAddress();
}
