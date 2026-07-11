CLI_JAR := build/cli.jar
CLI_BIN := build/kvctl
NODE_JAR := build/node.jar
MAIN := src/main/java/kvcluster/

.PHONY: build-node run-coordinator run-cli clean

build-node:
	jbang export fatjar --force -O $(NODE_JAR) $(MAIN)/NodeApplication.java

run-coordinator: build-node
	jbang Coordinator.java --nodes=5 --replication=2 --node-jar=$(NODE_JAR)

run-cli:
	jbang CLI.java $(ARGS)

clean:
	rm -rf build data

build-cli:
	jbang export fatjar --force -O $(CLI_JAR) $(MAIN)/CLIApplication.java
	printf '#!/usr/bin/env bash\nexec java -jar "$$(dirname "$$0")/cli.jar" "$$@"\n' > $(CLI_BIN)
	chmod +x $(CLI_BIN)
