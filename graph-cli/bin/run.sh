#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export MAVEN_HOME="${MAVEN_HOME:-/opt/apache-maven}"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

CONFIG_PATH="${1:-config.ini}"

mvn -q -DskipTests clean package
mvn -q exec:java -Dexec.args="$CONFIG_PATH"

