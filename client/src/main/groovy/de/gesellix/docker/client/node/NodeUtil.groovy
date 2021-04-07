package de.gesellix.docker.client.node

import de.gesellix.docker.client.system.ManageSystem
import groovy.util.logging.Slf4j

@Slf4j
class NodeUtil {

  ManageSystem manageSystem

  NodeUtil(ManageSystem manageSystem) {
    this.manageSystem = manageSystem
  }

  def resolveNodeId(nodeFilter) {
    def ownNodeId = {
      manageSystem.info().content.Swarm.NodeID
    }
    def resolve = { String ref ->
      (ref == "self") ? ownNodeId() : ref
    }
    def resolvedNodeFilter = nodeFilter
    if (nodeFilter instanceof String) {
      resolvedNodeFilter = resolve(nodeFilter)
    }
    else if (nodeFilter instanceof String[] || nodeFilter instanceof Collection) {
      resolvedNodeFilter = nodeFilter.collect { String ref ->
        resolve(ref)
      }
    }
    resolvedNodeFilter
  }
}
