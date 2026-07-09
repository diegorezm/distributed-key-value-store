### Replication
- [x] Extend `ConsistentHashRouter` to return a replica set (`routeFor(key, replicaCount)`)
- [ ] Coordinator informs each node who its replica peers are (on spawn, or via a lookup)
- [ ] Primary node forwards `PUT`/`DELETE` writes to its replica peers
- [ ] Decide consistency model: wait for all replica acks vs. fire-and-forget
- [ ] `GET` reads from primary only (for now) — revisit read-from-replica later if needed

### Failure Detection
- [ ] Background loop in `CoordinatorServer` that periodically calls `healthCheck` on every node
- [ ] Mark a node "dead" after N consecutive failed checks
- [ ] Remove dead node from `ConsistentHashRouter` ring
- [ ] (Optional) Auto-respawn a replacement node on the same port/id

### Persistence
- [ ] Simple append-only write-ahead log per node (`key,value\n` on every `PUT`/`DELETE`)
- [ ] Replay log on node startup to rebuild in-memory `HashMap`
- [ ] Decide on log file location per node (e.g. `./data/node-1.log`)

### Benchmarking
- [ ] Containerize nodes + coordinator (Docker), similar to `eljobs` setup
- [ ] Load test with `vegeta` (or similar) — throughput vs. node count
- [ ] Measure coordinator redirect overhead vs. direct node access
- [ ] Check key distribution/load balance across nodes under real traffic
- [ ] Python scripts to chart results (reuse pattern from `eljobs`)

### CLI Client
- [ ] Small CLI (`kvctl put/get/del/nodes`) talking to the coordinator
- [ ] Handles the `307` redirect automatically (so testing isn't just raw `http` calls)
- [ ] (Optional) explore building this in Go or Rust instead of Java

### Explicitly out of scope
- Data migration on membership change (ring rebalancing / key transfer between nodes)
