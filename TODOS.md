### Replication
- [x] Extend `ConsistentHashRouter` to return a replica set (`routeFor(key, replicaCount)`)
- [x] Coordinator informs each node who its replica peers are (on spawn, or via a lookup)
- [x] Primary node forwards `PUT`/`DELETE` writes to its replica peers
- [x] Decide consistency model: wait for all replica acks vs. fire-and-forget
- [x] `GET` reads from primary only (for now) — revisit read-from-replica later if needed
- [x] Coordinator redirects to a replica if the primary node is down (requires failure detection to know a node is down in the first place)

### Failure Detection
- [x] Background loop in `CoordinatorServer` that periodically calls `healthCheck` on every node
- [x] Mark a node "dead" after N consecutive failed checks
- [x] Remove dead node from `ConsistentHashRouter` ring
- [ ] (Optional) Auto-respawn a replacement node on the same port/id

### Persistence
- [x] Simple append-only write-ahead log per node (`key,value\n` on every `PUT`/`DELETE`)
- [x] Replay log on node startup to rebuild in-memory `HashMap`
- [x] Decide on log file location per node (e.g. `./data/node-1.log`)

### CLI Client
- [x] Small CLI (`kvctl put/get/del/nodes`) talking to the coordinator
- [x] Handles the `307` redirect automatically (so testing isn't just raw `http` calls)

### Benchmarking
- [ ] Containerize nodes + coordinator (Docker) 
- [ ] Load test 
- [ ] Measure coordinator redirect overhead vs. direct node access
- [ ] Check key distribution/load balance across nodes under real traffic
- [ ] Python scripts to chart results

### Explicitly out of scope
- Data migration on membership change (ring rebalancing / key transfer between nodes)
