SHELL := /usr/bin/env bash
.DEFAULT_GOAL := help

MVN ?= mvn
JAVA ?= java
HOST ?= localhost
PORT ?= 8080
CLI_ARGS ?=
SERVER_JAR := server/target/server-jar-with-dependencies.jar
CLIENT_JAR := client/target/client-jar-with-dependencies.jar
DB_CONFIG := server/src/main/resources/db.properties

.PHONY: help check check-java check-maven check-gui check-db build test test-engine test-cli verify server cli gui clean

help: ## Show the available commands
	@echo "Chess development commands"
	@echo
	@echo "  make server       Build and start the multiplayer server"
	@echo "  make cli          Build and start the command-line client"
	@echo "  make gui          Build and start the desktop client"
	@echo "  make test         Run the complete test suite (requires MySQL)"
	@echo "  make test-engine  Run the dependency-free chess rule tests"
	@echo "  make test-cli     Run CLI tests without MySQL or a desktop"
	@echo "  make verify       Compile everything and run the complete suite"
	@echo "  make build        Build runnable client and server jars"
	@echo
	@echo "Configuration: HOST=$(HOST) PORT=$(PORT)"
	@echo "Example: make gui HOST=192.168.1.20 PORT=8080"

check: check-java check-maven ## Check build dependencies
	@echo "All build dependencies are available."

check-java:
	@command -v "$(JAVA)" >/dev/null 2>&1 || { \
		echo "Error: Java is missing. Install JDK 21 or newer and ensure 'java' is on PATH." >&2; exit 2; }
	@major="$$($(JAVA) -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')"; \
		if [[ -z "$$major" || "$$major" -lt 21 ]]; then \
			echo "Error: Java 21 or newer is required; found version $${major:-unknown}." >&2; exit 2; \
		fi

check-maven:
	@command -v "$(MVN)" >/dev/null 2>&1 || { \
		echo "Error: Apache Maven is missing. Install Maven 3.8+ and ensure 'mvn' is on PATH." >&2; \
		echo "Ubuntu/Debian: sudo apt install maven" >&2; \
		echo "macOS: brew install maven" >&2; exit 2; }

check-gui:
	@if [[ "$$(uname -s)" == "Linux" && -z "$$DISPLAY" && -z "$$WAYLAND_DISPLAY" ]]; then \
		echo "Error: no graphical desktop was detected (DISPLAY and WAYLAND_DISPLAY are empty)." >&2; \
		echo "Run 'make cli' in a terminal, or run 'make gui' from a desktop session." >&2; exit 2; \
	fi

check-db:
	@if [[ ! -f "$(DB_CONFIG)" ]]; then \
		echo "Error: server database configuration is missing: $(DB_CONFIG)" >&2; \
		echo "Copy $(DB_CONFIG).example to $(DB_CONFIG), then set the MySQL host, port, database, user, and password." >&2; \
		echo "The configured MySQL service must be running before the server starts." >&2; exit 2; \
	fi
	@for key in db.host db.port db.name db.user db.password; do \
		grep -q "^$$key=" "$(DB_CONFIG)" || { echo "Error: $(DB_CONFIG) is missing '$$key'." >&2; exit 2; }; \
	done

build: check ## Build runnable client and server jars
	$(MVN) --no-transfer-progress -DskipTests package

test: check check-db ## Run all tests (database tests require configured MySQL)
	$(MVN) --no-transfer-progress test

test-engine: check ## Run chess rule tests without server/database dependencies
	$(MVN) --no-transfer-progress -pl shared test

test-cli: check ## Run terminal parsing, board interaction, and text gameplay tests
	$(MVN) --no-transfer-progress -pl client -am -Dtest=CliInputParserTests,CommandLineSelectionTests,TerminalControlsTests,BoardGraphicsTests,TerminalExperienceTests,GamePlayCliTests -Dsurefire.failIfNoSpecifiedTests=false -DargLine=-Djava.awt.headless=true test

verify: build test ## Build and run all tests

server: check check-db ## Start the server (PORT defaults to 8080)
	$(MVN) --no-transfer-progress -q -pl server -am -DskipTests package
	$(JAVA) -jar "$(SERVER_JAR)" "$(PORT)"

cli: check ## Start the terminal client (HOST/PORT select the server)
	$(MVN) --no-transfer-progress -q -pl client -am -DskipTests package
	$(JAVA) -cp "$(CLIENT_JAR)" ClientMain --host "$(HOST)" --port "$(PORT)" $(CLI_ARGS)

gui: check check-gui ## Start the desktop client (HOST/PORT select the server)
	$(MVN) --no-transfer-progress -q -pl client -am -DskipTests package
	$(JAVA) -cp "$(CLIENT_JAR)" GuiMain --host "$(HOST)" --port "$(PORT)"

clean: check-maven ## Remove Maven build output
	$(MVN) --no-transfer-progress clean
