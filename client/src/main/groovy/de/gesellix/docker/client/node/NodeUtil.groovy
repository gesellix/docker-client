package de.gesellix.docker.client.node

import de.gesellix.docker.client.system.ManageSystem

class NodeUtil {

  ManageSystem manageSystem

  NodeUtil(ManageSystem manageSystem) {
    this.manageSystem = manageSystem
  }

  def resolveNodeId(nodeFilter) {
    def ownNodeId = {
      manageSystem.info().content.swarm.nodeID
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
