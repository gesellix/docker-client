package de.gesellix.docker.client

interface ManageNode {

//    demote      Demote one or more nodes from manager in the swarm

    def demoteNodes(... node)

//    inspect     Display detailed information on one or more nodes

    def inspectNode(name)

//    ls          List nodes in the swarm

    def nodes()

    def nodes(query)

//    promote     Promote one or more nodes to manager in the swarm

    def promoteNodes(... node)

//    ps          List tasks running on one or more nodes, defaults to current node

    def tasksOnNode(node)

    def tasksOnNode(node, query)

//    rm          Remove one or more nodes from the swarm

    def rmNode(name)

//    update      Update a node

    def updateNode(name, query, config)
}
