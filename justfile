cli_jar  := "build/cli.jar"
cli_bin  := "build/kvctl"
node_jar := "build/node.jar"
main     := "src/main/java/kvcluster"

# Build the node fat-jar (required by coordinator)
build-node:
    mkdir -p build
    jbang export fatjar --force -O {{node_jar}} {{main}}/NodeApplication.java

# Build the CLI fat-jar + wrapper script
build-cli:
    mkdir -p build
    jbang export fatjar --force -O {{cli_jar}} {{main}}/CLIApplication.java
    printf '#!/usr/bin/env bash\nexec java -jar "$(dirname "$0")/cli.jar" "$@"\n' > {{cli_bin}}
    chmod +x {{cli_bin}}

# Build everything
build: build-node build-cli

# Run the coordinator (override nodes/replication as needed)
run-coordinator nodes="3" replication="1": build-node
    jbang {{main}}/CoordinatorApplication.java \
        --nodes={{nodes}} \
        --replication={{replication}} \
        --node-jar={{node_jar}}

# Run CLI commands: just cli get foo  /  just cli put foo bar
cli *args: build-cli
    {{cli_bin}} {{args}}

test: build-node
    jbang src/test/java/kvcluster/TestMain.java

test-class class:
    jbang src/test/java/kvcluster/TestMain.java --select-class {{class}}


clean:
    rm -rf build data
