package de.gesellix.docker.client.tasks

import de.gesellix.docker.engine.EngineResponse

interface ManageTask {

  EngineResponse tasks()

  EngineResponse tasks(query)

  EngineResponse inspectTask(name)
}
