Akka-RED
========

Akka port of [Node-RED](https://nodered.org/)

# Development

For the UI we currently reuse a running node-red instance where we have modified the ajax/websocket urls.

TODO: document the needed patches (/comms, /flows, ...)

# TODO

not in any particular order

* Add unit/http tests
* ci build (travis?)
* Actually start/run the flows (inject -> debug)
* Implement all required websocket communications
* See if we can host the UI ourselves (or proxy), that would avoid patching Node-RED
* Find a solution for writing nodes in scala + hosting their frontend in js files
* Find a suitable cron library (quartz vs akron)
* Modularize the project (runtime vs api vs frontend)
* Make this available as docker image
